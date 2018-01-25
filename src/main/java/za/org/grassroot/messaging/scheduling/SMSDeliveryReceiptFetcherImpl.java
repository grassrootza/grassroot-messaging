package za.org.grassroot.messaging.scheduling;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.service.sms.SMSDeliveryReceipt;
import za.org.grassroot.messaging.service.sms.SMSDeliveryStatus;
import za.org.grassroot.messaging.service.sms.SmsSendingService;
import za.org.grassroot.messaging.service.sms.aat.AatMsgStatus;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service @Slf4j
public class SMSDeliveryReceiptFetcherImpl implements SMSDeliveryReceiptFetcher {

    @Value("${grassroot.callbackq.enabled:false}") private boolean callbackQueueEnabled;
    @Value("${grassroot.callbackq.queue.name:delivery-receipt-dummy}") private String callbackQueueName;
    @Value("${grassroot.callbackq.interval:5000}") private long callbackInterval;
    @Value("${grassroot.callbackq.ratepersecond:1}") private int ratePerSecond;
    @Value("${grassroot.callbackq.queue.deadl:dead-letter}") private String deadLetterQueue;

    private AmazonSQS sqs;
    private int maxSqsMessages;

    private NotificationBroker notificationBroker;
    private Map<MessagingProvider, SmsSendingService> messagingProviderServiceMap = new HashMap<>();

    private AtomicBoolean running = new AtomicBoolean(false);

    public SMSDeliveryReceiptFetcherImpl(NotificationBroker notificationBroker, @Qualifier("aatSmsSender") SmsSendingService aatSendingService) {
        this.notificationBroker = notificationBroker;
        this.messagingProviderServiceMap.put(MessagingProvider.AAT, aatSendingService);
    }

    @PostConstruct
    public void init() {
        if (callbackQueueEnabled) {
            setUpSqsClient();
        }
    }

    private void setUpSqsClient() {
        try {
            this.sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();
            this.maxSqsMessages = Math.min(10, (int) ((callbackInterval / 1000) * ratePerSecond));
            log.info("max SQS messages = {}", maxSqsMessages);
        } catch (SdkClientException e) {
            log.error("Could not set up SQS client but callback q enabled", e);
        }
    }

    @Override
    public void fetchDeliveryReceiptsFromApiLog() {
        log.info("called fetch deliver receipts");
        // if not running set to running and do the job
        if (running.compareAndSet(false, true)) {
            log.info("Running SMSDeliveryReceiptFetcher ...");
            try {
                for (Map.Entry<MessagingProvider, SmsSendingService> entry : messagingProviderServiceMap.entrySet()) {
                    MessagingProvider provider = entry.getKey();
                    SmsSendingService smsSendingService = entry.getValue();
                    List<Notification> notifications = notificationBroker.loadSentNotificationsWithUnknownDeliveryStatus(provider);
                    for (Notification notification : notifications) {
                        try {
                            SMSDeliveryReceipt receipt = smsSendingService.fetchSMSDeliveryStatus(notification.getSendingKey());
                            if (receipt != null) {
                                SMSDeliveryStatus deliveryStatus = receipt.getDeliveryStatus();
                                if (deliveryStatus == SMSDeliveryStatus.DELIVERED)
                                    notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERED, null, false, true, null, null);
                                else if (deliveryStatus == SMSDeliveryStatus.DELIVERY_FAILED)
                                    notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED, receipt.getDescription(), false, true, null, null);
                            } else {
                                log.warn("No delivery receipt returned by provider for notification uid {}, sendingKey: {}", notification.getUid(), notification.getSendingKey());
                                notificationBroker.incrementReceiptFetchCount(notification.getUid());
                            }
                        } catch (Exception e) {
                            log.error("Failed to fetch delivery receipt for notification with uid" + notification.getUid(), e);
                        }
                    }
                }
            } catch (Exception e) {
                log.info("SMSDeliveryReceiptFetcher failed!", e);
            } finally {
                running.set(false);
            }
        } else {
            log.warn("SMSDeliveryReceiptFetcher triggered but it's already running");
        }

    }

    @Override
    public void clearCallBackQueue() {
        if (callbackQueueEnabled && sqs != null) {
            log.debug("clearing the call back queue ... max messages : {}", maxSqsMessages);
            ReceiveMessageRequest request = new ReceiveMessageRequest(callbackQueueName);
            request.setMaxNumberOfMessages(maxSqsMessages);
            List<Message> batch = sqs.receiveMessage(request).getMessages();
            log.info("received {} messages in queue ...", batch.size());
            batch.forEach(this::handleCallbackDeliveryReceipt);
        }
    }

    private void handleCallbackDeliveryReceipt(Message message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            log.debug("processing SQS message: {}", message);
            String msgBody = message.getBody();
            Map<String, Object> msgBodyMap = objectMapper.readValue(msgBody, new TypeReference<HashMap<String, Object>>() {});
            log.debug("msgBodyMap: {}", msgBodyMap);
            LinkedHashMap qsm = (LinkedHashMap) msgBodyMap.get("queryStringParameters");
            log.debug("queryStringParams, type: {}, content: {}", qsm.getClass(), qsm.toString());
            handleReceipt((String) qsm.get("rf"), Integer.valueOf((String) qsm.get("st")));
        } catch (IOException e) {
            log.error("could not read value", e);
            sendToDeadLetterQueue(message);
        } catch (ClassCastException|NumberFormatException e) {
            log.error("could not cast map from JSON, time to shift transforming to lambda", e);
            sendToDeadLetterQueue(message);
        } finally {
            log.debug("cleaning up by removing message");
            sqs.deleteMessage(callbackQueueName, message.getReceiptHandle());
        }
    }

    private void handleReceipt(String messageKey, Integer status) {
        log.info("looking for notification with key: {}, status: {}", messageKey, status);
        Notification notification = notificationBroker.loadBySendingKey(messageKey);
        if (notification != null) {
            AatMsgStatus aatMsgStatus = AatMsgStatus.fromCode(status);
            if (aatMsgStatus != null) {
                SMSDeliveryStatus deliveryStatus = aatMsgStatus.toSMSDeliveryStatus();
                if (deliveryStatus == SMSDeliveryStatus.DELIVERED)
                    notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERED, null, false, true, null, null);
                else if (deliveryStatus == SMSDeliveryStatus.DELIVERY_FAILED)
                    notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED, "Message delivery failed: " + aatMsgStatus.name(),
                            false, true, null, null);
            } else {
                // maybe also send to DL queue
                log.error("Received confusing AAT message status: {}", status);
            }
        }
    }

    private void sendToDeadLetterQueue(Message message) {
        sqs.sendMessage(deadLetterQueue, message.getBody());
    }
}
