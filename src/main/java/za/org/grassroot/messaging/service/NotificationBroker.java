package za.org.grassroot.messaging.service;

import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;

import java.util.List;

/**
 * Created by paballo on 2016/04/07.
 */
public interface NotificationBroker {

    int MAX_SENDING_ATTEMPTS = 3;

	Notification loadNotification(String uid);

    List<Notification> loadNextBatchOfNotificationsToSend(int maxCount);

	List<Notification> loadUnreadNotificationsToSend();

    List<Notification> loadSentNotificationsWithUnknownDeliveryStatus(MessagingProvider messagingProvider);

	MessageAndRoutingBundle loadRoutingBundle(String notificationUid);

    /**
     * updates notification status
     *
     * @param notificationUid        uid of notification
     * @param status                 status to be set
     * @param errorMessage           if status is being set to some type of delivery failure status, error message should be set also
     * @param resultOfSendingAttempt if this status update is result of sending attempt this should be true, false otherwise
     * @param messageSendKey         if this status update is result of sending attempt sending provider message identifier should be passed here, null otherwise
     * @param sentViaProvider        if this status update is result of sending attempt sending provider should be specified, null otherwise
     */
    void updateNotificationStatus(String notificationUid, NotificationStatus status,
                                  String errorMessage, boolean resultOfSendingAttempt,
                                  String messageSendKey, MessagingProvider sentViaProvider);


}
