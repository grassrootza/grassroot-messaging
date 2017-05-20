package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.NotificationRepository;
import za.org.grassroot.messaging.service.NotificationBroker;

import java.time.Instant;
import java.util.List;

@Service
public class BatchedNotificationSenderImpl implements BatchedNotificationSender {
	private final Logger logger = LoggerFactory.getLogger(BatchedNotificationSenderImpl.class);

	private final NotificationRepository notificationRepository;
	private final NotificationBroker notificationBroker;

	@Autowired
	public BatchedNotificationSenderImpl(NotificationRepository notificationRepository, NotificationBroker notificationBroker) {
		this.notificationRepository = notificationRepository;
		this.notificationBroker = notificationBroker;
	}

	/**
	 * Processed in non-transacted manner because we want to process each notification in separate transaction.
	 */
	@Override
	@Transactional(readOnly = true)
	public void processPendingNotifications() {
		Instant time = Instant.now();
		logger.info("checking for notifications ...");
		List<Notification> notifications = notificationRepository.findFirst75ByNextAttemptTimeBeforeOrderByNextAttemptTimeAsc(time);
		if (notifications.size() > 0) {
			logger.info("Sending {} registered notification(s)", notifications.size());
		}
		notifications.forEach(n -> notificationBroker.sendNotification(n.getUid()));
	}
}