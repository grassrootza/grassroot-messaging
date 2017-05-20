package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.NotificationRepository;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.service.MessageSendingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Ported by luke on 2017/05/19.
 */
@Service
public class UnreadNotificationHandlerImpl implements UnreadNotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnreadNotificationHandler.class);

    private final NotificationRepository notificationRepository;
    private final MessageSendingService messageSendingManager;

    @Autowired
    public UnreadNotificationHandlerImpl(NotificationRepository notificationRepository, MessageSendingService messageSendingManager) {
        this.notificationRepository = notificationRepository;
        this.messageSendingManager = messageSendingManager;
    }

    @Override
    @Transactional
    public void processUnreadNotifications() {
        logger.info("Processing unread notifications ...");
        Instant timeToCheck = Instant.now().minus(10, ChronoUnit.MINUTES);

        // need to only check for those attempt, else may send before user has chance to view
        // note : do the check on read, not viewed on android, because we want to preserve that as false but mark to read on SMS send (to avoid repeat deliveries)
        List<Notification> unreadNotifications = notificationRepository
                .findFirst100ByReadFalseAndAttemptCountGreaterThanAndLastAttemptTimeGreaterThan(0, timeToCheck);
        if (unreadNotifications.size() > 0) {
            logger.info("Sending {} unread notifications", unreadNotifications.size());
        }

        for (Notification notification : unreadNotifications) {
            logger.info("Routing notification to SMS ...");
            messageSendingManager.sendMessage(UserMessagingPreference.SMS.name(), notification);
        }
    }

}
