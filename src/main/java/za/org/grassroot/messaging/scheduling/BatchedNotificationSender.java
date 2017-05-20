package za.org.grassroot.messaging.scheduling;

import org.springframework.stereotype.Service;

@Service
public interface BatchedNotificationSender {
	void processPendingNotifications();
}