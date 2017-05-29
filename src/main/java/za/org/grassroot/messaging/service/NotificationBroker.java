package za.org.grassroot.messaging.service;

import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;

import java.util.List;
import java.util.Set;

/**
 * Created by paballo on 2016/04/07.
 */
public interface NotificationBroker {

	Notification loadNotification(String uid);

	List<Notification> loadNextBatchOfNotificationsToSend();

	List<Notification> loadUnreadNotificationsToSend();

	MessageAndRoutingBundle loadRoutingBundle(String notificationUid);

	// increments attempt time while sending
	Notification loadNotificationForSending(String notificationUid);

	void updateNotificationReadStatus(String notificationUid, boolean read);

	void updateNotificationsViewedAndRead(Set<String> notificationUids);

	void markNotificationAsDelivered(String notificationUid);

	void markNotificationAsFailedGcmDelivery(String notificationUid);

}
