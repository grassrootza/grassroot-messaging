package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.enums.UserMessagingPreference;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class NotificationSpecifications {

    public static Specifications<Notification> getUnsuccessfulNotifications() {

        Specification<Notification> sendFailed = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.SENDING_FAILED);
        Specification<Notification> deliveryFailed = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.DELIVERY_FAILED);
        Specification<Notification> messageSent = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.SENT);
        Specification<Notification> androidChannel = (root, query, cb) -> cb.equal(root.get("deliveryChannel"), UserMessagingPreference.ANDROID_APP);
        Instant tenMinAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        Specification<Notification> sentAtLeast10MinAgo = (root, query, cb) -> cb.lessThan(root.get("lastStatusChange"), tenMinAgo);

        Specifications<Notification> unreadMessageOnAndroid = Specifications.where(messageSent).and(androidChannel).and(sentAtLeast10MinAgo);

        return unreadMessageOnAndroid.or(sendFailed).or(deliveryFailed);

    }

    public static Specifications<Notification> getSentNotificationsWithUnknownDeliveryStatus(MessagingProvider sentViaProvider) {

        Specification<Notification> sent = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.SENT);
        Specification<Notification> providerMatch = (root, query, cb) -> cb.equal(root.get("sentViaProvider"), sentViaProvider);

        Instant aMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        Specification<Notification> sentAtLeast10MinAgo = (root, query, cb) -> cb.lessThan(root.get("lastStatusChange"), aMinuteAgo);

        return Specifications.where(sent).and(providerMatch).and(sentAtLeast10MinAgo);

    }
}
