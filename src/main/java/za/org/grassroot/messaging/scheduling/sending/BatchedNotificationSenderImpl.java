package za.org.grassroot.messaging.scheduling.sending;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.ConfigVariable;
import za.org.grassroot.core.domain.GcmRegistration;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.core.repository.ConfigRepository;
import za.org.grassroot.core.repository.GcmRegistrationRepository;
import za.org.grassroot.messaging.service.NotificationBroker;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class BatchedNotificationSenderImpl implements BatchedNotificationSender {

	private static final int DEFAULT_BATCH_SIZE = 100;

	private final Logger logger = LoggerFactory.getLogger(BatchedNotificationSenderImpl.class);

	private final NotificationBroker notificationBroker;
	private final GcmRegistrationRepository gcmRegistrationRepository;
	private final ConfigRepository configRepository;

	private MessageChannel requestChannel;

	private static final List<DeliveryRoute> EMAIL_ROUTES = Arrays.asList(DeliveryRoute.EMAIL_3RDPARTY,
			DeliveryRoute.EMAIL_GRASSROOT, DeliveryRoute.EMAIL_USERACCOUNT);

	@Autowired
	public BatchedNotificationSenderImpl(NotificationBroker notificationBroker, GcmRegistrationRepository gcmRegistrationRepository, ConfigRepository configRepository) {
		this.notificationBroker = notificationBroker;
		this.gcmRegistrationRepository = gcmRegistrationRepository;
		this.configRepository = configRepository;
	}

	@Autowired
	public void setRequestChannel(@Qualifier("outboundRouterChannel") MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	public int getBatchSize() {
		Optional<ConfigVariable> batchSize = configRepository.findOneByKey("notification.send.batch.size");
		logger.info("Found config var? : {}", batchSize.map(ConfigVariable::getValue).orElse("Not found"));
		if (batchSize.isPresent() && StringUtils.isNumeric(batchSize.get().getValue())) {
			logger.debug("Batch size present and passes number");
			return Integer.parseInt(batchSize.get().getValue());
		} else {
			logger.debug("No batch size found, or is not a number, returning default");
			return DEFAULT_BATCH_SIZE;
		}
	}

	/**
	 * Processed in non-transacted manner because we want to process each notification in
	 * separate transaction.
	 */
	@Override
	public void processPendingNotifications() {

		logger.info("processing pending notifications ...");
		List<Notification> notifications = notificationBroker.loadNextBatchOfNotificationsToSend(getBatchSize());
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
			requestChannel.send(createMessage(notification, notification.getDeliveryChannel()));
        } catch (Exception e) {
			logger.error("Failed to send notification {}, : {}", notification, e);
		}
	}

	private Message<Notification> createMessage(Notification notification, DeliveryRoute givenRoute) {

		DeliveryRoute route = (givenRoute != null) ? givenRoute : (notification.getTarget().getMessagingPreference() == null) ?
						DeliveryRoute.SMS : notification.getTarget().getMessagingPreference();

        if (DeliveryRoute.ANDROID_APP.equals(route)) {
        	logger.info("sending via Android App route");
			GcmRegistration registration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(notification.getTarget());
			if (registration == null)
				route = DeliveryRoute.SHORT_MESSAGE;
		}


		return MessageBuilder.withPayload(notification)
				.setHeader("route", safeRoute(notification.getTarget(), route).name())
				.build();
	}

	// todo : include Android / GCM
	private DeliveryRoute safeRoute(User target, DeliveryRoute preference) {
		if (DeliveryRoute.SMS.equals(preference) && !target.hasPhoneNumber()) {
			return target.hasEmailAddress() ? DeliveryRoute.EMAIL_GRASSROOT : DeliveryRoute.WEB_ONLY; // since we then don't know what to do with it
		} else if (EMAIL_ROUTES.contains(preference) && !target.hasEmailAddress()) {
			return target.hasPhoneNumber() ? DeliveryRoute.SMS : DeliveryRoute.WEB_ONLY; // as above
		} else {
			return preference;
		}
	}

}
