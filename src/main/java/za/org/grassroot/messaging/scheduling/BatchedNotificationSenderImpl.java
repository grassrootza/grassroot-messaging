package za.org.grassroot.messaging.scheduling;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.GcmRegistration;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.repository.GcmRegistrationRepository;
import za.org.grassroot.messaging.service.NotificationBroker;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

@Service
public class BatchedNotificationSenderImpl implements BatchedNotificationSender {
	private final Logger logger = LoggerFactory.getLogger(BatchedNotificationSenderImpl.class);

	private final NotificationBroker notificationBroker;
	private final GcmRegistrationRepository gcmRegistrationRepository;

	private MessageChannel requestChannel;

	@Autowired
	public BatchedNotificationSenderImpl(NotificationBroker notificationBroker, EntityManager entityManager, GcmRegistrationRepository gcmRegistrationRepository) {
		this.notificationBroker = notificationBroker;
		this.gcmRegistrationRepository = gcmRegistrationRepository;
	}

	@Autowired
	public void setRequestChannel(@Qualifier("outboundRouterChannel") MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	/**
	 * Processed in non-transacted manner because we want to process each notification in
	 * separate transaction.
	 */
	@Timed
	@Override
	public void processPendingNotifications() {

		List<Notification> notifications = notificationBroker.loadNextBatchOfNotificationsToSend(150);
		if (notifications.size() > 0) {
			logger.info("Sending {} registered notification(s)", notifications.size());
		}
		notifications.forEach(n -> sendNotification(n.getUid()));
	}

	private void sendNotification(String notificationUid) {

		Objects.requireNonNull(notificationUid);

        Notification notification = notificationBroker.loadNotification(notificationUid);
        logger.debug("Sending notification: {}", notification);
		try {
            requestChannel.send(createMessage(notification, notification.deliveryChannel.toString()));
        } catch (Exception e) {
			logger.error("Failed to send notification {}, : {}", notification, e);
		}
	}

	private Message<Notification> createMessage(Notification notification, String givenRoute) {


        String route = (givenRoute != null) ? givenRoute :
				(notification.getTarget().getMessagingPreference() == null) ?
						"SMS" : notification.getTarget().getMessagingPreference().name();

        if ("ANDROID_APP".equals(route)) {
			GcmRegistration registration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(notification.getTarget());
			if (registration == null)
				route = "SMS";
		}

		return MessageBuilder.withPayload(notification)
				.setHeader("route", route)
				.build();
	}

}