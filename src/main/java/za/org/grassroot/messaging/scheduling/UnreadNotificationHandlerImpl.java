package za.org.grassroot.messaging.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.service.NotificationBroker;

import java.util.List;

/**
 * Ported by luke on 2017/05/19.
 */
@Service
public class UnreadNotificationHandlerImpl implements UnreadNotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnreadNotificationHandler.class);

    private final NotificationBroker notificationBroker;

    private MessageChannel requestChannel;

    @Autowired
    public UnreadNotificationHandlerImpl(NotificationBroker notificationBroker) {
        this.notificationBroker = notificationBroker;
    }

    @Autowired
    public void setRequestChannel(@Qualifier("outboundRouterChannel") MessageChannel requestChannel) {
        this.requestChannel = requestChannel;
    }

    @Override
    @Transactional
    public void processUnreadNotifications() {
        logger.info("Processing unread notifications ...");
        List<Notification> unreadNotifications = notificationBroker.loadUnreadNotificationsToSend();
        if (unreadNotifications.size() > 0) {
            logger.info("Sending {} unread notifications", unreadNotifications.size());
            unreadNotifications.forEach(n -> {
                logger.debug("Routing notification to SMS ...");
                MessageAndRoutingBundle bundle = notificationBroker.loadRoutingBundle(n.getUid());
                bundle.setRoutePreference(UserMessagingPreference.SMS);
                requestChannel.send(MessageBuilder.withPayload(bundle).setHeader("route", "SMS").build());
            });
        }
    }

}
