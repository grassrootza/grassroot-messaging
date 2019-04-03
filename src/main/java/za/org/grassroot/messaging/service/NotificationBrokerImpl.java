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
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.Notification_;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.domain.group.Group;
import za.org.grassroot.core.domain.group.GroupJoinMethod;
import za.org.grassroot.core.domain.group.Membership;
import za.org.grassroot.core.domain.notification.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.repository.MembershipRepository;
import za.org.grassroot.core.repository.NotificationRepository;
import za.org.grassroot.messaging.domain.NotificationSpecifications;

import java.util.*;

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
        Objects.requireNonNull(uid);
        return notificationRepository.findByUid(uid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> loadNextBatchOfNotificationsToSend(int maxCount) {
        return wrapMaxResults(NotificationSpecifications.getNotificationsReadyForSending(), maxCount);
    }


    @Override
    public List<Notification> loadUnreadGcmNotificationsToSend(int maxCount) {
        return wrapMaxResults(NotificationSpecifications.getUnreadAndroidNotifications(), maxCount);
    }

    @Override
    public List<Notification> loadFailedShortMessagesToTryAgain(int maxCount) {
        return wrapMaxResults(NotificationSpecifications.getUnsuccessfulSmsNotifications(), maxCount);
    }

    private List<Notification> wrapMaxResults(Specification<Notification> specs, int maxCount) {
        Pageable pageable = PageRequest.of(0, maxCount, Sort.Direction.ASC, Notification_.lastStatusChange.getName());
        logger.debug("Fetching specs: {}, pageable: {}", specs, pageable);
        Page<Notification> page = notificationRepository.findAll(specs, pageable);
        return page.hasContent() ? page.getContent() : Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> loadSentNotificationsWithUnknownDeliveryStatus(MessagingProvider messagingProvider) {
        return notificationRepository.findAll(NotificationSpecifications.getSentNotificationsWithUnknownDeliveryStatus(messagingProvider));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> loadBySendingKey(String sendingKey) {
        return notificationRepository.findOne((root, query, cb) -> cb.equal(root.get(Notification_.sendingKey), sendingKey));
    }


    @Override
    @Transactional
    public void updateNotificationStatus(String notificationUid, NotificationStatus status, String errorMessage,
                                         boolean resultOfSendingAttempt, boolean resultOfReceiptFetch, String messageSendKey, MessagingProvider sentViaProvider) {

        Notification notification = notificationRepository.findByUid(Objects.requireNonNull(notificationUid));
        if (notification == null) {
            logger.error("Error! Passed a notification ID without notification: {}", notificationUid);
            return;
        }

        notification.updateStatus(status, resultOfSendingAttempt, resultOfReceiptFetch, errorMessage);

        if (messageSendKey != null)
            notification.setSendingKey(messageSendKey);
        if (sentViaProvider != null)
            notification.setSentViaProvider(sentViaProvider);

        if (NotificationStatus.UNDELIVERABLE.equals(status)) {
            User user = notification.getTarget();
            user.setContactError(true);
        }

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void updateNotifications(Set<Notification> notificationSet) {
        logger.info("updating status for {} notifications", notificationSet.size());
        notificationRepository.saveAll(notificationSet);
    }

    @Override
    @Transactional
    public void incrementReceiptFetchCount(String notificationUid) {
        Notification notification = notificationRepository.findByUid(notificationUid);
        if (notification != null) {
            notification.incrementReceiptFetchCount();
            notificationRepository.save(notification);
        }
    }

    @Override
    public boolean isUserSelfJoinedToGroup(Notification notification) {
        User user = notification.getTarget();
        Group group = notification.getRelevantGroup();
        if (group != null) {
            Specification<Membership> spec = NotificationSpecifications.getMembership(group.getUid(), user.getUid());
            Optional<Membership> membership = membershipRepository.findOne(spec);
            if (membership.isPresent())
                return membership.get().getJoinMethod() == GroupJoinMethod.SELF_JOINED;
        }
        return false;
    }


}