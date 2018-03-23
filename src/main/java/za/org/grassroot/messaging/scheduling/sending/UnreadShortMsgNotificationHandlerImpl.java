package za.org.grassroot.messaging.scheduling.sending;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.util.SendTimeUtil;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;

/**
 * Ported by luke on 2017/05/19.
 */
@Service
public class UnreadShortMsgNotificationHandlerImpl implements UnreadShortMsgNotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnreadShortMsgNotificationHandler.class);
    private static final ZoneId userZone = ZoneId.of("Africa/Johannesburg");

    private final NotificationBroker notificationBroker;

    @Autowired
    public UnreadShortMsgNotificationHandlerImpl(NotificationBroker notificationBroker) {
        this.notificationBroker = notificationBroker;
    }


    @Override
    public void processUnreadNotifications() {
        logger.info("Processing unread notifications ...");
        List<Notification> unreadNotifications = notificationBroker.loadUnreadGcmNotificationsToSend(50);
        logger.info("Send time would be restricted to: {}", SendTimeUtil.restrictSendTime(userZone));
        if (unreadNotifications.size() > 0) {
            logger.info("Sending {} unread notifications", unreadNotifications.size());
            int sendCount = 0;
            int maxCount = 0;
            for (Notification n : unreadNotifications) {
                if (n.getSendAttempts() < NotificationBroker.MAX_SENDING_ATTEMPTS) {
                    sendCount++;
                    logger.info("Updating previously failed GCM message {} for resend ...", n.getUid());
                    n.setDeliveryChannel(DeliveryRoute.SMS);
                    n.setSendOnlyAfter(SendTimeUtil.restrictSendTime(userZone));
                    n.updateStatus(NotificationStatus.READY_FOR_SENDING, false, false, null);
                } else {
                    maxCount++;
                    logger.info("Max delivery attempts tried for notification {}, time to give up", n.getUid());
                    n.updateStatus(NotificationStatus.UNDELIVERABLE, false, false, null);
                }
            }
            logger.info("{} messages queued for resend, {} messages marked as failed", sendCount, maxCount);
            notificationBroker.updateNotifications(new HashSet<>(unreadNotifications));
        }
    }

}
