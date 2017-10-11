package za.org.grassroot.messaging.service;

import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;

import java.util.List;

/**
 * Created by paballo on 2016/04/07.
 */
public interface NotificationBroker {

    int MAX_SENDING_ATTEMPTS = 3;

	Notification loadNotification(String uid);

	List<Notification> loadNextBatchOfNotificationsToSend();

	List<Notification> loadUnreadNotificationsToSend();

	MessageAndRoutingBundle loadRoutingBundle(String notificationUid);

    void updateNotificationStatus(String notificationUid, NotificationStatus status, String errorMessage, String messageSendKey);




}
