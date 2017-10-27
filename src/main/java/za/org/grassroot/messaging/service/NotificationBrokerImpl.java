package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.domain.Notification_;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.repository.NotificationRepository;
import za.org.grassroot.messaging.domain.repository.NotificationSpecifications;

import java.util.Collections;
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
        return notificationRepository.findByUid(uid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> loadNextBatchOfNotificationsToSend(int maxCount) {
        Pageable pageable = new PageRequest(0, maxCount, Sort.Direction.ASC, Notification_.createdDateTime.getName());
        Page<Notification> page = notificationRepository.findAll(NotificationSpecifications.getNotificationsReadyForSending(), pageable);
        return page.hasContent() ? page.getContent() : Collections.emptyList();
    }


    @Override
    public List<Notification> loadUnreadNotificationsToSend() {

        return notificationRepository.findAll(NotificationSpecifications.getUnsuccessfulNotifications());
    }

    @Override
    public List<Notification> loadSentNotificationsWithUnknownDeliveryStatus(MessagingProvider messagingProvider) {
        return notificationRepository.findAll(NotificationSpecifications.getSentNotificationsWithUnknownDeliveryStatus(messagingProvider));
    }


    @Override
    @Transactional
    public void updateNotificationStatus(String notificationUid, NotificationStatus status, String errorMessage,
                                         boolean resultOfSendingAttempt, String messageSendKey, MessagingProvider sentViaProvider) {

        Notification notification = notificationRepository.findByUid(notificationUid);

        if (notification != null) {

            notification.updateStatus(status, resultOfSendingAttempt, errorMessage);

            if (messageSendKey != null)
                notification.setSendingKey(messageSendKey);
            if (sentViaProvider != null)
                notification.setSentViaProvider(sentViaProvider);
        }
    }



}