package za.org.grassroot.messaging.domain;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import za.org.grassroot.core.domain.*;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class NotificationSpecifications {

    public static Specifications<Notification> getUnreadAndroidNotifications() {

        Specification<Notification> messageNotRead = (root, query, cb) -> cb.notEqual(root.get("status"), NotificationStatus.READ);
        Specification<Notification> messageNotUndeliverable = (root, query, cb) -> cb.notEqual(root.get("status"), NotificationStatus.UNDELIVERABLE);
        Specification<Notification> messageNotReadyForSending = (root, query, cb) -> cb.notEqual(root.get("status"), NotificationStatus.READY_FOR_SENDING);
        Specification<Notification> androidChannel = (root, query, cb) -> cb.equal(root.get("deliveryChannel"), DeliveryRoute.ANDROID_APP);

        return Specifications
                .where(messageNotRead)
                .and(messageNotUndeliverable)
                .and(messageNotReadyForSending)
                .and(androidChannel)
                .and(lastStatusChangeNotStale(1))
                .and(sentAtLeastXMinsAgo(30));
    }

    public static Specifications<Notification> getUnsuccessfulSmsNotifications() {
        Specification<Notification> shortMessageOrGcm = (root, query, cb) -> root.get("deliveryChannel").in(
                DeliveryRoute.SHORT_MESSAGE, DeliveryRoute.SMS, DeliveryRoute.WHATSAPP, DeliveryRoute.ANDROID_APP
        );
        Specification<Notification> sendFailedOrDeliveryFailed = (root, query, cb) -> root.get("status").in(
                NotificationStatus.SENDING_FAILED, NotificationStatus.DELIVERY_FAILED);

        return Specifications
                .where(shortMessageOrGcm)
                .and(sendFailedOrDeliveryFailed)
                .and(sentAtLeastXMinsAgo(15));
    }

    private static Specification<Notification> sentAtLeastXMinsAgo(int X) {
        Instant tenMinAgo = Instant.now().minus(X, ChronoUnit.MINUTES);
        return (root, query, cb) -> cb.lessThan(root.get("lastStatusChange"), tenMinAgo);
    }

    private static Specification<Notification> lastStatusChangeNotStale(int daysBack) {
        Instant daysAgo = Instant.now().minus(daysBack, ChronoUnit.DAYS);
        return (root, query, cb) -> cb.greaterThan(root.get("lastStatusChange"), daysAgo);
    }

    public static Specifications<Notification> getSentNotificationsWithUnknownDeliveryStatus(MessagingProvider sentViaProvider) {

        Specification<Notification> sent = (root, query, cb) -> cb.equal(root.get(Notification_.status), NotificationStatus.SENT);
        Specification<Notification> providerMatch = (root, query, cb) -> cb.equal(root.get(Notification_.sentViaProvider), sentViaProvider);
        Specification<Notification> lowFetches = (root, query, cb)
                -> cb.lessThan(root.get(Notification_.readReceiptFetchAttempts), NotificationBroker.MAX_RECEIPT_FETCHES);

        Instant tenMinutesAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        Specification<Notification> sentAtLeast10MinAgo = (root, query, cb) -> cb.lessThan(root.get(Notification_.lastStatusChange), tenMinutesAgo);

        return Specifications.where(sent).and(providerMatch).and(sentAtLeast10MinAgo).and(lowFetches);

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
