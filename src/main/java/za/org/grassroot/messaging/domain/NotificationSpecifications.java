package za.org.grassroot.messaging.domain;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import za.org.grassroot.core.domain.*;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.core.enums.MessagingProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class NotificationSpecifications {

    public static Specifications<Notification> getUnsuccessfulNotifications() {

        Specification<Notification> sendFailed = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.SENDING_FAILED);
        Specification<Notification> deliveryFailed = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.DELIVERY_FAILED);
        Specification<Notification> messageSent = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.SENT);
        Specification<Notification> androidChannel = (root, query, cb) -> cb.equal(root.get("deliveryChannel"), DeliveryRoute.ANDROID_APP);
        Instant tenMinAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        Specification<Notification> sentAtLeast10MinAgo = (root, query, cb) -> cb.lessThan(root.get("lastStatusChange"), tenMinAgo);

        Specifications<Notification> unreadMessageOnAndroid = Specifications.where(messageSent).and(androidChannel).and(sentAtLeast10MinAgo);

        return unreadMessageOnAndroid.or(sendFailed).or(deliveryFailed);

    }

    public static Specifications<Notification> getSentNotificationsWithUnknownDeliveryStatus(MessagingProvider sentViaProvider) {

        Specification<Notification> sent = (root, query, cb) -> cb.equal(root.get(Notification_.status), NotificationStatus.SENT);
        Specification<Notification> providerMatch = (root, query, cb) -> cb.equal(root.get(Notification_.sentViaProvider), sentViaProvider);

        Instant tenMinutesAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        Specification<Notification> sentAtLeast10MinAgo = (root, query, cb) -> cb.lessThan(root.get(Notification_.lastStatusChange), tenMinutesAgo);

        return Specifications.where(sent).and(providerMatch).and(sentAtLeast10MinAgo);

    }


    public static Specifications<Notification> getNotificationsReadyForSending() {

        Specification<Notification> readyStatus = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.READY_FOR_SENDING);

        Instant now = Instant.now();
        Specification<Notification> sendOnlyAfterIsNull = (root, query, cb) -> cb.isNull(root.get("sendOnlyAfter"));
        Specification<Notification> sendOnlyAfterIsInPast = (root, query, cb) -> cb.lessThan(root.get("sendOnlyAfter"), now);
        Specifications<Notification> sendOnlyAfterOK = Specifications.where(sendOnlyAfterIsNull).or(sendOnlyAfterIsInPast);


        return Specifications.where(readyStatus).and(readyStatus).and(sendOnlyAfterOK);
    }

    public static Specification<Membership> getMembership(String groupUid, String userUid) {

        Specification<Membership> groupMembership = (root, query, cb) -> cb.equal(root.get(Membership_.group).get(Group_.uid), groupUid);
        Specification<Membership> userMembership = (root, query, cb) -> cb.equal(root.get(Membership_.user).get(User_.uid), userUid);

        return Specifications.where(groupMembership).and(userMembership);
    }
}
