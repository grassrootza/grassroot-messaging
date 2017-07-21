package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import za.org.grassroot.messaging.domain.GcmRegistration;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.domain.repository.GcmRegistrationRepository;
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
	 * Processed in non-transacted manner because we want to process each notification in separate transaction.
	 */
	@Override
	public void processPendingNotifications() {
		List<Notification> notifications = notificationBroker.loadNextBatchOfNotificationsToSend();
		if (notifications.size() > 0) {
			logger.info("Sending {} registered notification(s)", notifications.size());
		}
		notifications.forEach(n -> sendNotification(n.getUid()));
	}

	private void sendNotification(String notificationUid) {
		Objects.requireNonNull(notificationUid);

		Notification notification = notificationBroker.loadNotificationForSending(notificationUid);
		logger.debug("Sending notification: {}", notification);

		// todo: fix up Android and other routing in general fixes, for now use timeline as key
		try {
			boolean redelivery = notification.getAttemptCount() > 1;
			if (redelivery || !notification.isForAndroidTimeline()) {
			    logger.info("redelivering a notification, attempt count: {}", notification.getAttemptCount());
				// notification.setNextAttemptTime(null); // this practically means we try to redeliver only once
				requestChannel.send(createMessage(notification, "SMS"));
			} else {
				requestChannel.send(createMessage(notification, null));
			}
		} catch (Exception e) {
			logger.error("Failed to send notification {}, : {}", notification, e);
		}
	}

	private Message<MessageAndRoutingBundle> createMessage(Notification notification, String givenRoute) {
		MessageAndRoutingBundle routingBundle = notificationBroker.loadRoutingBundle(notification.getUid());
		String route = (givenRoute != null) ? givenRoute :
				(routingBundle == null || routingBundle.getRoutePreference() == null) ?
						"SMS" : routingBundle.getRoutePreference().name();
		if ("ANDROID_APP".equals(route)) {
			GcmRegistration registration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(notification.getTarget());
			if (registration != null) {
				routingBundle.setGcmRegistrationId(registration.getRegistrationId());
			} else {
				routingBundle.setRoutePreference(UserMessagingPreference.SMS);
				route = "SMS";
			}
		}

		return MessageBuilder.withPayload(routingBundle)
				.setHeader("route", route)
				.build();
	}

}