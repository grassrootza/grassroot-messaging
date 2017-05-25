package za.org.grassroot.messaging.service;

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
import za.org.grassroot.messaging.util.DebugUtil;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ported to micro project by Luke on 2017/05/18.
 */

@Service
public class NotificationBrokerImpl implements NotificationBroker {

    private final static Logger logger = LoggerFactory.getLogger(NotificationBrokerImpl.class);

    private final MessageChannel requestChannel;

    private final NotificationRepository notificationRepository;
    private final GcmRegistrationRepository gcmRegistrationRepository;
    private final EntityManager entityManager;

    @Autowired
    public NotificationBrokerImpl(@Qualifier("outboundRouterChannel") MessageChannel requestChannel,
                                  NotificationRepository notificationRepository,
                                  GcmRegistrationRepository gcmRegistrationRepository,
                                  EntityManager entityManager) {
        this.requestChannel = requestChannel;
        this.gcmRegistrationRepository = gcmRegistrationRepository;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Notification loadNotification(String uid) {
        Objects.nonNull(uid);
        return notificationRepository.findOneByUid(uid);
    }

    @Override
    @Transactional
    public void updateNotificationReadStatus(String notificationUid, boolean read) {
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        notification.setRead(read);
    }

    @Override
    @Transactional
    public void updateNotificationsViewedAndRead(Set<String> notificationUids) {
        List<Notification> notifications = notificationRepository.findByUidIn(notificationUids);
        notifications.forEach(Notification::markReadAndViewed);
    }

    @Override
    @Transactional
    public void markNotificationAsDelivered(String notificationUid) {
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        if (notification != null) {
            notification.markAsDelivered();
        } else {
            logger.info("No notification under UID {}, possibly from another environment", notificationUid);
        }
    }

    /* todo : fix this (switch to better use of error channels etc) to remove circular bean dependency */
    @Override
    @Transactional
    public void sendNotification(String notificationUid) {
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
            logger.error("Failed to send notification " + notification + ": " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void resendFailedGcmMessage(String notificationUid) {
        logger.info("notification broker, resending SMS");
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        requestChannel.send(createMessage(notification, "SMS"));
    }

    private MessageAndRoutingBundle getNotificationRouting(Notification notification) {
        Group group = notification.getGroupDescendantLog().getGroupDescendant().getAncestorGroup();
        return entityManager.createQuery("SELECT NEW za.org.grassroot.messaging.domain.MessageAndRoutingBundle(" +
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
                .setParameter("notification", notification)
                .getResultList().iterator().next();
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
}