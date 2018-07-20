package za.org.grassroot.messaging.scheduling.receipts;

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
import za.org.grassroot.core.domain.notification.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.service.sms.SMSDeliveryStatus;
import za.org.grassroot.messaging.service.sms.SmsSendingService;
import za.org.grassroot.messaging.service.sms.aat.AatMsgStatus;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
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
        Optional<Notification> notification = notificationBroker.loadBySendingKey(messageKey);
        if (notification.isPresent()) {
            AatMsgStatus aatMsgStatus = AatMsgStatus.fromCode(status);
            if (aatMsgStatus != null) {
                updateNotificationFromAatStatus(notification.get(), aatMsgStatus);
            } else {
                log.error("Received confusing AAT message status: {}", status);// maybe also send to DL queue
            }
        }
    }

    private void updateNotificationFromAatStatus(Notification notification, AatMsgStatus aatMsgStatus) {
        SMSDeliveryStatus deliveryStatus = aatMsgStatus.toSMSDeliveryStatus();
        if (deliveryStatus == SMSDeliveryStatus.DELIVERED) {
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERED, null,
                    false, true, null, MessagingProvider.AAT);
        } else if (deliveryStatus == SMSDeliveryStatus.DELIVERY_FAILED) {
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED, "Message delivery failed: " + aatMsgStatus.name(),
                    false, true, null, MessagingProvider.AAT);
        } else if (deliveryStatus == SMSDeliveryStatus.PROBLEM_NUMBER) {
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.UNDELIVERABLE, "Likely number issues: " + aatMsgStatus.name(),
                    false, true, null, MessagingProvider.AAT);
        }
    }

    private void sendToDeadLetterQueue(Message message) {
        sqs.sendMessage(deadLetterQueue, message.getBody());
    }
}
