package za.org.grassroot.messaging.service;

import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.NotificationStatus;

import java.util.List;

/**
 * Created by paballo on 2016/04/07.
 */
public interface NotificationBroker {

	Notification loadNotification(String uid);

	List<Notification> loadNextBatchOfNotificationsToSend();

	List<Notification> loadUnreadNotificationsToSend();

	MessageAndRoutingBundle loadRoutingBundle(String notificationUid);

	void updateNotificationStatus(String notificationUid, NotificationStatus status, String errorMessage);




}
