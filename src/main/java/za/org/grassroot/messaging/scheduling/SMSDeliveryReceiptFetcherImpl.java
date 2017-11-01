package za.org.grassroot.messaging.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.service.sms.SMSDeliveryReceipt;
import za.org.grassroot.messaging.service.sms.SMSDeliveryStatus;
import za.org.grassroot.messaging.service.sms.SmsSendingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class SMSDeliveryReceiptFetcherImpl implements SMSDeliveryReceiptFetcher {


    private NotificationBroker notificationBroker;
    private Map<MessagingProvider, SmsSendingService> messagingProviderServiceMap = new HashMap<>();

    private AtomicBoolean running = new AtomicBoolean(false);


    public SMSDeliveryReceiptFetcherImpl(NotificationBroker notificationBroker, @Qualifier("aatSmsSender") SmsSendingService aatSendingService) {
        this.notificationBroker = notificationBroker;
        this.messagingProviderServiceMap.put(MessagingProvider.AAT, aatSendingService);
    }

    @Override
    public void fetchDeliveryReceipts() {

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
                                    notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERED, null, false, null, null);
                                else if (deliveryStatus == SMSDeliveryStatus.DELIVERY_FAILED)
                                    notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED, receipt.getDescription(), false, null, null);
                            } else
                                log.warn("No delivery receipt returned by provider for notification uid {}, sendingKey: {}", notification.getUid(), notification.getSendingKey());

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
}
