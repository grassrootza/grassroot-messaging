package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.messaging.service.NotificationBroker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Ported by luke on 2017/05/19.
 */
@Service
public class UnsuccessfulSmsHandlerImpl implements UnsuccessfulSmsHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnreadShortMsgNotificationHandler.class);

    private final NotificationBroker notificationBroker;

    @Autowired
    public UnsuccessfulSmsHandlerImpl(NotificationBroker notificationBroker) {
        this.notificationBroker = notificationBroker;
    }


    @Override
    @Transactional
    public void processUnsuccessfulSmsMessages() {
        Instant maxAgeForRetry = Instant.now().minus(NotificationBroker.MAX_AGE_TO_RETRY_DAYS, ChronoUnit.DAYS);
        List<Notification> failedSmsMessages = notificationBroker.loadFailedShortMessagesToTryAgain();
        logger.info("Processing {} unsuccessful SMS messages ...", failedSmsMessages.size());
        failedSmsMessages.forEach(n -> {
            Instant threshold = n.getSendOnlyAfter() == null ? n.getCreatedDateTime() : n.getSendOnlyAfter();
            if (n.getSendAttempts() < NotificationBroker.MAX_SENDING_ATTEMPTS && threshold.isAfter(maxAgeForRetry)) {
                logger.info("Trying once more to resend message ... {}", n.getUid());
                n.setDeliveryChannel(DeliveryRoute.SMS);
                n.updateStatus(NotificationStatus.READY_FOR_SENDING, false, null);
            } else {
                logger.info("Max delivery attempts tried for notification {}, time to give up", n.getUid());
                n.updateStatus(NotificationStatus.UNDELIVERABLE, false, null);
            }
        });
    }

}