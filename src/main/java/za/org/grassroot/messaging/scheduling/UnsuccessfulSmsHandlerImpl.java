package za.org.grassroot.messaging.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.util.SendTimeUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

/**
 * Ported by luke on 2017/05/19.
 */
@Service @Slf4j
public class UnsuccessfulSmsHandlerImpl implements UnsuccessfulSmsHandler {

    private static final ZoneId userZone = ZoneId.of("Africa/Johannesburg");

    private final NotificationBroker notificationBroker;

    @Autowired
    public UnsuccessfulSmsHandlerImpl(NotificationBroker notificationBroker) {
        this.notificationBroker = notificationBroker;
    }


    @Override
    public void processUnsuccessfulSmsMessages() {
        Instant maxAgeForRetry = Instant.now().minus(NotificationBroker.MAX_AGE_TO_RETRY_DAYS, ChronoUnit.DAYS);
        List<Notification> failedSmsMessages = notificationBroker.loadFailedShortMessagesToTryAgain(150);
        log.info("Processing {} unsuccessful SMS messages ...", failedSmsMessages.size());
        int resend = 0;
        int markFailed = 0;
        for (Notification n : failedSmsMessages) {
            Instant threshold = n.getSendOnlyAfter() == null ? n.getCreatedDateTime() : n.getSendOnlyAfter();
            if (n.getSendAttempts() < NotificationBroker.MAX_SENDING_ATTEMPTS && threshold.isAfter(maxAgeForRetry)) {
                log.debug("Trying once more to resend message ... {}", n.getUid());
                resend++;
                n.setSendOnlyAfter(SendTimeUtil.restrictSendTime(userZone));
                n.setDeliveryChannel(DeliveryRoute.SMS);
                n.updateStatus(NotificationStatus.READY_FOR_SENDING, false, false, null);
            } else {
                log.debug("Max delivery attempts tried for notification {}, time to give up", n.getUid());
                markFailed++;
                n.updateStatus(NotificationStatus.UNDELIVERABLE, false, false, null);
            }
        }
        log.info("{} unsuccessful messages marked to resend, {} as failed", resend, markFailed);
        notificationBroker.updateNotifications(new HashSet<>(failedSmsMessages));
    }

}