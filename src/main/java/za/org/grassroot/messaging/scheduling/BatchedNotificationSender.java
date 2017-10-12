package za.org.grassroot.messaging.scheduling;


public interface BatchedNotificationSender {
	void processPendingNotifications();
}