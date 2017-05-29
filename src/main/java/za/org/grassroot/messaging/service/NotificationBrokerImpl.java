package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.repository.NotificationRepository;

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
        if (notification != null) {
            notification.incrementAttemptCount();
        } else {
            logger.info("No notification under UID {}, possibly from another environment", notificationUid);
        }
    }
}