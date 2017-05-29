package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.Notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Created by luke on 2017/05/17.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Notification findOneByUid(String uid);

    List<Notification> findByUidIn(Collection<String> uids);

    List<Notification> findFirst75ByNextAttemptTimeBeforeOrderByNextAttemptTimeAsc(Instant time);

    @Transactional(readOnly = true)
    List<Notification> findFirst100ByReadFalseAndAttemptCountGreaterThanAndLastAttemptTimeGreaterThan(int minAttemptCount, Instant lastAttemptTime);

    @Query("SELECT NEW za.org.grassroot.messaging.domain.MessageAndRoutingBundle(" +
            "n.uid, u.phoneNumber, n.message, u.messagingPreference, " +
            "(case when " +
            "   sum(case when log.userLogType = 'USED_A_JOIN_CODE' and log.description = :groupUid then 1 else 0 end) " +
            "> 0 then true else false end))" +
            "FROM Notification n " +
            "INNER JOIN n.target u " +
            "INNER JOIN u.userLogs log " +
            "WHERE n = :notification " +
            "GROUP BY n.uid, u.phoneNumber, n.message, u.messagingPreference")
    MessageAndRoutingBundle loadMessageAndRoutingBundle(@Param("groupUid") String groupUid, @Param("notification") Notification notification);

}
