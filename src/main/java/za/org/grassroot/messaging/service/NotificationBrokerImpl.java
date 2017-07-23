package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.Group;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.repository.NotificationRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ported to micro project by Luke on 2017/05/18.
 */

@Service
public class NotificationBrokerImpl implements NotificationBroker {

    private final static Logger logger = LoggerFactory.getLogger(NotificationBrokerImpl.class);

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationBrokerImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Notification loadNotification(String uid) {
        Objects.nonNull(uid);
        return notificationRepository.findOneByUid(uid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> loadNextBatchOfNotificationsToSend() {
        Instant time = Instant.now();
        return notificationRepository.findFirst150ByNextAttemptTimeBeforeOrderByNextAttemptTimeAsc(time);
    }

    @Override
    public List<Notification> loadUnreadNotificationsToSend() {
        // need to only check for those attempt, else may send before user has chance to view
        // note : do the check on read, not viewed on android, because we want to preserve that as false but mark to read on SMS send (to avoid repeat deliveries)
        Instant timeToCheck = Instant.now().minus(10, ChronoUnit.MINUTES);
        return notificationRepository
                .findFirst150ByReadFalseAndAttemptCountGreaterThanAndLastAttemptTimeGreaterThan(0, timeToCheck);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageAndRoutingBundle loadRoutingBundle(String notificationUid) {
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        if (!notification.isTaskRelated()) {
            // since all user log messages are on request
            return returnDefaultBundle(notification);
        } else {
            Group group = notification.hasGroupLog() ? notification.getGroupLog().getGroup() :
                    notification.getGroupDescendantLog().getGroupDescendant().getAncestorGroup();
            if (group == null) {
                logger.error("Error! Notification query gave null group, on: {}", notification);
                return returnDefaultBundle(notification);
            } else {
                MessageAndRoutingBundle bundle = notificationRepository.loadMessageAndRoutingBundle(group.getUid(),
                            notification);
                if (bundle == null) {
                    logger.error("Note! The notification routing query returned null, notification: {}", notification);
                    return returnDefaultBundle(notification);
                } else {
                    return bundle;
                }
            }
        }
    }

    @Override
    @Transactional
    public Notification loadNotificationForSending(String notificationUid) {
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        notification.incrementAttemptCount();
        notification.setLastAttemptTime(Instant.now());
        // we set next attempt (redelivery) time which will get erased in case delivery gets confirmed in the mean time
        notification.setNextAttemptTime(Instant.now().plusSeconds(60 * 5));
        return notification;
    }

    private MessageAndRoutingBundle returnDefaultBundle(Notification notification) {
        return new MessageAndRoutingBundle(notification.getUid(),
                notification.getTarget().getPhoneNumber(),
                notification.getMessage(),
                notification.getTarget().getMessagingPreference(),
                false);
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

    @Override
    @Transactional
    public void markNotificationAsFailedGcmDelivery(String notificationUid) {
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        if (notification != null && notification.getAttemptCount() <= 1) {
            // todo : might want to change user preference, etc
            notification.incrementAttemptCount();
        } else {
            logger.info("No notification under UID {}, possibly from another environment", notificationUid);
        }
    }
}