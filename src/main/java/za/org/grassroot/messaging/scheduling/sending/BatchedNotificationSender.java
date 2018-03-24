package za.org.grassroot.messaging.scheduling.sending;


public interface BatchedNotificationSender {
	void processPendingNotifications();
}