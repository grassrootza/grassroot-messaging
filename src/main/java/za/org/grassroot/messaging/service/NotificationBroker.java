package za.org.grassroot.messaging.service;

import za.org.grassroot.messaging.domain.Notification;

import java.util.Set;

/**
 * Created by paballo on 2016/04/07.
 */
public interface NotificationBroker {

	Notification loadNotification(String uid);

	void updateNotificationReadStatus(String notificationUid, boolean read);

	void updateNotificationsViewedAndRead(Set<String> notificationUids);

	void markNotificationAsDelivered(String notificationUid);

}
