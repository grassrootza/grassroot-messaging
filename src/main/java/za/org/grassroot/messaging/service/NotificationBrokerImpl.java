package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.Group;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.domain.task.TaskLog;
import za.org.grassroot.core.enums.NotificationType;
import za.org.grassroot.core.repository.NotificationRepository;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.repository.MessageAndRoutingBundleRepository;
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
    private final MessageAndRoutingBundleRepository messageAndRoutingBundleRepository;

    @Autowired
    public NotificationBrokerImpl(NotificationRepository notificationRepository, MessageAndRoutingBundleRepository messageAndRoutingBundleRepository) {
        this.notificationRepository = notificationRepository;
        this.messageAndRoutingBundleRepository = messageAndRoutingBundleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Notification loadNotification(String uid) {
        Objects.nonNull(uid);
        return notificationRepository.findByUid(uid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> loadNextBatchOfNotificationsToSend() {

        return notificationRepository.findFirst150ByStatusOrderByCreatedDateTimeAsc(NotificationStatus.READY_FOR_SENDING);
    }


    @Override
    public List<Notification> loadUnreadNotificationsToSend() {

        return notificationRepository.findAll(NotificationSpecifications.getUnsuccessfulNotifications());
    }


    @Override
    @Transactional(readOnly = true)
    public MessageAndRoutingBundle loadRoutingBundle(String notificationUid) {
        Notification notification = notificationRepository.findByUid(notificationUid);
        boolean taskRelated = notification.getEventLog() != null || notification.getTodoLog() != null;
        if (!taskRelated) {
            // since all user log messages are on request
            return returnDefaultBundle(notification);
        } else {

            Group group = notification.getGroupLog() != null ? notification.getGroupLog().getGroup() :
                    getGroupDescendantLog(notification).getTask().getAncestorGroup();
            if (group == null) {
                logger.error("Error! Notification query gave null group, on: {}", notification);
                return returnDefaultBundle(notification);
            } else {
                int userJoinViaCodeInGroupLogCount = messageAndRoutingBundleRepository.getUserLogsWithJoinCode(group.getUid());

                MessageAndRoutingBundle bundle = new MessageAndRoutingBundle(
                        notification.getUid(),
                        notification.getTarget().getPhoneNumber(),
                        notification.getMessage(),
                        notification.getTarget().getMessagingPreference(),
                        userJoinViaCodeInGroupLogCount > 0);

                if (bundle == null) {
                    logger.error("Note! The notification routing query returned null, notification: {}", notification);
                    return returnDefaultBundle(notification);
                } else {
                    return bundle;
                }
            }
        }
    }

    private TaskLog getGroupDescendantLog(Notification notification) {
        if (NotificationType.EVENT.equals(notification.getNotificationType())) {
            return notification.getEventLog();
        } else if (NotificationType.TODO.equals(notification.getNotificationType())) {
            return notification.getTodoLog();
        } else {
            throw new IllegalArgumentException("Cannot obtain group descendant log from non-task log");
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
        Notification notification = notificationRepository.findByUid(notificationUid);
        if (notification != null) {
            notification.updateStatus(status);
            if (messageSendKey != null)
                notification.setSendingKey(messageSendKey);
        }
    }



}