package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.GcmRegistration;
import za.org.grassroot.messaging.domain.Group;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.domain.repository.GcmRegistrationRepository;
import za.org.grassroot.messaging.domain.repository.NotificationRepository;
import za.org.grassroot.messaging.service.NotificationBrokerImpl;
import za.org.grassroot.messaging.util.DebugUtil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class BatchedNotificationSenderImpl implements BatchedNotificationSender {
	private final Logger logger = LoggerFactory.getLogger(BatchedNotificationSenderImpl.class);

	private final NotificationRepository notificationRepository;
	private final EntityManager entityManager;
	private final GcmRegistrationRepository gcmRegistrationRepository;

	private MessageChannel requestChannel;

	@Autowired
	public BatchedNotificationSenderImpl(NotificationRepository notificationRepository, EntityManager entityManager, GcmRegistrationRepository gcmRegistrationRepository) {
		this.notificationRepository = notificationRepository;
		this.entityManager = entityManager;
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
	@Transactional(readOnly = true)
	public void processPendingNotifications() {
		Instant time = Instant.now();
		List<Notification> notifications = notificationRepository.findFirst75ByNextAttemptTimeBeforeOrderByNextAttemptTimeAsc(time);
		if (notifications.size() > 0) {
			logger.info("Sending {} registered notification(s)", notifications.size());
		}
		notifications.forEach(n -> sendNotification(n.getUid()));
	}

	private void sendNotification(String notificationUid) {
		Objects.requireNonNull(notificationUid);
		DebugUtil.transactionRequired(NotificationBrokerImpl.class);

		Instant now = Instant.now();

		Notification notification = notificationRepository.findOneByUid(notificationUid);
		logger.debug("Sending notification: {}", notification);

		notification.incrementAttemptCount();
		notification.setLastAttemptTime(now);

		try {
			boolean redelivery = notification.getAttemptCount() > 1;
			if (redelivery) {
				// notification.setNextAttemptTime(null); // this practically means we try to redeliver only once
				requestChannel.send(createMessage(notification, "SMS"));
			} else {
				// we set next attempt (redelivery) time which will get erased in case delivery gets confirmed in the mean time
				notification.setNextAttemptTime(now.plusSeconds(60 * 5));
				requestChannel.send(createMessage(notification, null));
			}
		} catch (Exception e) {
			logger.error("Failed to send notification {}, : {}", notification, e);
		}
	}

	private Message<MessageAndRoutingBundle> createMessage(Notification notification, String givenRoute) {
		MessageAndRoutingBundle routingBundle;
		if (notification.isTaskRelated()) {
			routingBundle = getNotificationRouting(notification);
		} else {
			// since all user log messages are on request
			routingBundle = new MessageAndRoutingBundle(notification.getUid(),
					notification.getTarget().getPhoneNumber(),
					notification.getMessage(),
					notification.getTarget().getMessagingPreference(), true);
		}

		String route = (givenRoute == null) ? routingBundle.getRoutePreference().name() : givenRoute;
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

	private MessageAndRoutingBundle getNotificationRouting(Notification notification) {
		Group group = notification.hasGroupLog() ? notification.getGroupLog().getGroup() :
				notification.getGroupDescendantLog().getGroupDescendant().getAncestorGroup();
        TypedQuery<MessageAndRoutingBundle> query = entityManager.createQuery("SELECT NEW za.org.grassroot.messaging.domain.MessageAndRoutingBundle(" +
				"n.uid, u.phoneNumber, n.message, u.messagingPreference, " +
				"(case when " +
				"   sum(case when log.userLogType = 'USED_A_JOIN_CODE' and log.description = :groupUid then 1 else 0 end) " +
				"> 0 then true else false end))" +
				"FROM Notification n " +
				"INNER JOIN n.target u " +
				"INNER JOIN u.userLogs log " +
				"WHERE n = :notification " +
				"GROUP BY n.uid, u.phoneNumber, n.message, u.messagingPreference", MessageAndRoutingBundle.class)
				.setParameter("groupUid", group.getUid())
				.setParameter("notification", notification);
        List<MessageAndRoutingBundle> list = query.getResultList();
        if (list != null && !list.isEmpty()) {
            return list.iterator().next();
        } else {
            logger.error("Error! The notification routing query returned null");
            return new MessageAndRoutingBundle(notification.getUid(), notification.getTarget().getPhoneNumber(),
                    notification.getMessage(), UserMessagingPreference.SMS, false);
        }
	}
}