package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.*;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.repository.MembershipRepository;
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
    private final MembershipRepository membershipRepository;

    @Autowired
    public NotificationBrokerImpl(NotificationRepository notificationRepository, MembershipRepository membershipRepository) {
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
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
    @Transactional(readOnly = true)
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
            notificationRepository.save(notification);
        }
    }

    @Override
    public boolean isUserSelfJoinedToGroup(Notification notification) {
        User user = notification.getTarget();
        Group group = notification.getRelevantGroup();
        if (group != null) {
            Specification<Membership> spec = NotificationSpecifications.getMembership(group.getUid(), user.getUid());
            Membership membership = membershipRepository.findOne(spec);
            if (membership != null)
                return membership.getJoinMethod() == GroupJoinMethod.SELF_JOINED;
        }
        return false;
    }





}