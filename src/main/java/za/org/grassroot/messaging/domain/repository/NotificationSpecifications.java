package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.NotificationStatus;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;

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

        Specifications<Notification> unreadMessageOnAndroid = Specifications.where(messageSent).or(androidChannel).and(sentAtLeast10MinAgo);

        return unreadMessageOnAndroid.or(sendFailed).or(deliveryFailed);

    }
}
