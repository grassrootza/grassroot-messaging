package za.org.grassroot.messaging.domain;

import org.springframework.data.jpa.domain.Specification;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.Notification_;
import za.org.grassroot.core.domain.User_;
import za.org.grassroot.core.domain.group.Group_;
import za.org.grassroot.core.domain.group.Membership;
import za.org.grassroot.core.domain.group.Membership_;
import za.org.grassroot.core.domain.notification.NotificationStatus;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class NotificationSpecifications {

    public static Specification<Notification> getUnreadAndroidNotifications() {

        Specification<Notification> messageNotRead = (root, query, cb) -> cb.notEqual(root.get("status"), NotificationStatus.READ);
        Specification<Notification> messageNotUndeliverable = (root, query, cb) -> cb.notEqual(root.get("status"), NotificationStatus.UNDELIVERABLE);
        Specification<Notification> messageNotReadyForSending = (root, query, cb) -> cb.notEqual(root.get("status"), NotificationStatus.READY_FOR_SENDING);
        Specification<Notification> androidChannel = (root, query, cb) -> cb.equal(root.get("deliveryChannel"), DeliveryRoute.ANDROID_APP);
        Specification<Notification> notSentByAat = (root, query, cb) -> cb.notEqual(root.get("sentViaProvider"), MessagingProvider.AAT);

        return Specification
                .where(messageNotRead)
                .and(messageNotUndeliverable)
                .and(messageNotReadyForSending)
                .and(androidChannel)
                .and(notSentByAat) // since sometimes routing header can be GCM but defaults into AAT
                .and(lastStatusChangeNotStale(1))
                .and(sentAtLeastXMinsAgo(30));
    }

    public static Specification<Notification> getUnsuccessfulSmsNotifications() {
        Specification<Notification> shortMessageOrGcm = (root, query, cb) -> root.get("deliveryChannel").in(
                DeliveryRoute.SHORT_MESSAGE, DeliveryRoute.SMS, DeliveryRoute.WHATSAPP, DeliveryRoute.ANDROID_APP
        );
        Specification<Notification> sendFailedOrDeliveryFailed = (root, query, cb) -> root.get("status").in(
                NotificationStatus.SENDING_FAILED, NotificationStatus.DELIVERY_FAILED);

        return Specification
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

    public static Specification<Notification> getSentNotificationsWithUnknownDeliveryStatus(MessagingProvider sentViaProvider) {

        Specification<Notification> sent = (root, query, cb) -> cb.equal(root.get(Notification_.status), NotificationStatus.SENT);
        Specification<Notification> providerMatch = (root, query, cb) -> cb.equal(root.get(Notification_.sentViaProvider), sentViaProvider);
        Specification<Notification> lowFetches = (root, query, cb)
                -> cb.lessThan(root.get(Notification_.readReceiptFetchAttempts), NotificationBroker.MAX_RECEIPT_FETCHES);

        Instant tenMinutesAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        Specification<Notification> sentAtLeast10MinAgo = (root, query, cb) -> cb.lessThan(root.get(Notification_.lastStatusChange), tenMinutesAgo);

        return Specification.where(sent).and(providerMatch).and(sentAtLeast10MinAgo).and(lowFetches);

    }


    public static Specification<Notification> getNotificationsReadyForSending() {

        Specification<Notification> readyStatus = (root, query, cb) -> cb.equal(root.get("status"), NotificationStatus.READY_FOR_SENDING);

        Instant now = Instant.now();
        Specification<Notification> sendOnlyAfterIsNull = (root, query, cb) -> cb.isNull(root.get("sendOnlyAfter"));
        Specification<Notification> sendOnlyAfterIsInPast = (root, query, cb) -> cb.lessThan(root.get("sendOnlyAfter"), now);
        Specification<Notification> sendOnlyAfterOK = Specification.where(sendOnlyAfterIsNull).or(sendOnlyAfterIsInPast);


        return Specification.where(readyStatus).and(sendOnlyAfterOK);
    }

    public static Specification<Membership> getMembership(String groupUid, String userUid) {

        Specification<Membership> groupMembership = (root, query, cb) -> cb.equal(root.get(Membership_.group).get(Group_.uid), groupUid);
        Specification<Membership> userMembership = (root, query, cb) -> cb.equal(root.get(Membership_.user).get(User_.uid), userUid);

        return Specification.where(groupMembership).and(userMembership);
    }
}
