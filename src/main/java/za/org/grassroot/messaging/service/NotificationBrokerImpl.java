package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.NotificationRepository;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.util.DebugUtil;

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

    private final NotificationRepository notificationRepository;
    private final MessageSendingService messageSendingService;

    @Autowired
    public NotificationBrokerImpl(NotificationRepository notificationRepository, MessageSendingService messageSendingService) {
        this.notificationRepository = notificationRepository;
        this.messageSendingService = messageSendingService;
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
        logger.debug("Setting notification as read ...");
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
    public void sendNotification(String notificationUid) {
        Objects.requireNonNull(notificationUid);
        DebugUtil.transactionRequired(NotificationBrokerImpl.class);

        Instant now = Instant.now();

        Notification notification = notificationRepository.findOneByUid(notificationUid);
        logger.info("Sending notification: {}", notification);

        notification.incrementAttemptCount();
        notification.setLastAttemptTime(now);

        /* todo : fix this (switch to better use of error channels etc) to remove circular bean dependency */
        try {
            boolean redelivery = notification.getAttemptCount() > 1;
            if (redelivery) {
                // notification.setNextAttemptTime(null); // this practically means we try to redeliver only once
                messageSendingService.sendMessage(UserMessagingPreference.SMS.name(), notification);
            } else {
                // we set next attempt (redelivery) time which will get erased in case delivery gets confirmed in the mean time
                notification.setNextAttemptTime(now.plusSeconds(60 * 15));
                messageSendingService.sendMessage(notification);
            }
        } catch (Exception e) {
            logger.error("Failed to send notification " + notification + ": " + e.getMessage(), e);
        }
    }
}
