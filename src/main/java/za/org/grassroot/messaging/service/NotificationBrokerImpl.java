package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.Group;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.NotificationStatus;
import za.org.grassroot.messaging.domain.repository.NotificationRepository;
import za.org.grassroot.messaging.domain.repository.NotificationSpecifications;

import java.util.List;
import java.util.Objects;

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

        return notificationRepository.findFirst150ByStatusOrderByCreatedDateTimeAsc(NotificationStatus.READY_TO_SEND);
    }


    @Override
    public List<Notification> loadUnreadNotificationsToSend() {

        return notificationRepository.findAll(NotificationSpecifications.getUnsuccessfulNotifications());
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


    private MessageAndRoutingBundle returnDefaultBundle(Notification notification) {
        return new MessageAndRoutingBundle(notification.getUid(),
                notification.getTarget().getPhoneNumber(),
                notification.getMessage(),
                notification.getTarget().getMessagingPreference(),
                false);
    }

    @Override
    @Transactional
    public void updateNotificationStatus(String notificationUid, NotificationStatus status, String errorMessage, String messageSendKey) {
        Notification notification = notificationRepository.findOneByUid(notificationUid);
        if (notification != null) {
            notification.updateStatus(status);
            if (messageSendKey != null)
                notification.setSendingKey(messageSendKey);
        }
    }



}