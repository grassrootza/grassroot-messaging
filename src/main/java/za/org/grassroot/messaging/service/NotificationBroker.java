package za.org.grassroot.messaging.service;

import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;

import java.util.List;

/**
 * Created by paballo on 2016/04/07.
 */
public interface NotificationBroker {

    int MAX_SENDING_ATTEMPTS = 2;
    int MAX_AGE_TO_RETRY_DAYS = 2;
    int MAX_RECEIPT_FETCHES = 3;

	Notification loadNotification(String uid);

    List<Notification> loadNextBatchOfNotificationsToSend(int maxCount);

	List<Notification> loadUnreadGcmNotificationsToSend();

	List<Notification> loadFailedShortMessagesToTryAgain();

    List<Notification> loadSentNotificationsWithUnknownDeliveryStatus(MessagingProvider messagingProvider);

    Notification loadBySendingKey(String sendingKey);

    /**
     * updates notification status
     *  @param notificationUid        uid of notification
     * @param status                 status to be set
     * @param errorMessage           if status is being set to some type of delivery failure status, error message should be set also
     * @param resultOfSendingAttempt if this status update is result of sending attempt this should be true, false otherwise
     * @param resultOfReceiptFetch   if this status update is result of getting a receipt via querying logs or callback, set as true
     * @param messageSendKey         if this status update is result of sending attempt sending provider message identifier should be passed here, null otherwise
     * @param sentViaProvider        if this status update is result of sending attempt sending provider should be specified, null otherwise
     */
    void updateNotificationStatus(String notificationUid, NotificationStatus status,
                                  String errorMessage, boolean resultOfSendingAttempt, boolean resultOfReceiptFetch,
                                  String messageSendKey, MessagingProvider sentViaProvider);

    void incrementReceiptFetchCount(String notificationUid);

    boolean isUserSelfJoinedToGroup(Notification notification);

}
