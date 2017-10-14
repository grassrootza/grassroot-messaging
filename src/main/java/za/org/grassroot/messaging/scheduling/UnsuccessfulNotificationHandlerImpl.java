package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.UserMessagingPreference;
import za.org.grassroot.messaging.service.NotificationBroker;

import java.util.List;

/**
 * Ported by luke on 2017/05/19.
 */
@Service
public class UnsuccessfulNotificationHandlerImpl implements UnsuccessfulNotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnsuccessfulNotificationHandler.class);

    private final NotificationBroker notificationBroker;

    @Autowired
    public UnsuccessfulNotificationHandlerImpl(NotificationBroker notificationBroker) {
        this.notificationBroker = notificationBroker;
    }


    @Override
    @Transactional
    public void processUnreadNotifications() {
        logger.info("Processing unread notifications ...");
        List<Notification> unreadNotifications = notificationBroker.loadUnreadNotificationsToSend();
        if (unreadNotifications.size() > 0) {
            logger.info("Sending {} unread notifications", unreadNotifications.size());
            unreadNotifications.forEach(n -> {
                if (n.getSendAttempts() < NotificationBroker.MAX_SENDING_ATTEMPTS) {
                    logger.debug("Updating message {} for resend ...", n.getUid());
                    n.setDeliveryChannel(UserMessagingPreference.SMS);
                    n.updateStatus(NotificationStatus.READY_FOR_SENDING, false, null);
                } else {
                    logger.debug("Max delivery attempts tried for notification {}, time to give up", n.getUid());
                    n.updateStatus(NotificationStatus.UNDELIVERABLE, false, null);
                }
            });
        }
    }

}
