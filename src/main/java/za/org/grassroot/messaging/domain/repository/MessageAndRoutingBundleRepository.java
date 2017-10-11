package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.org.grassroot.core.domain.Notification;

public interface MessageAndRoutingBundleRepository extends JpaRepository<Notification, Long> {

//    @Query("SELECT NEW za.org.grassroot.messaging.domain.MessageAndRoutingBundle(" +
//            "n.uid, u.phoneNumber, n.message, u.messagingPreference, " +
//            "(case when " +
//            "   sum(case when log.userLogType = 'USED_A_JOIN_CODE' and log.description = :groupUid then 1 else 0 end) " +
//            "> 0 then true else false end))" +
//            "FROM Notification n " +
//            "INNER JOIN n.target u " +
//            "LEFT JOIN u.userLogs log " +
//            "WHERE n = :notification " +
//            "GROUP BY n.uid, u.phoneNumber, n.message, u.messagingPreference")
//    MessageAndRoutingBundle loadMessageAndRoutingBundle(@Param("groupUid") String groupUid, @Param("notification") Notification notification);


    //todo(beegor) check with Luke if this is OK
    @Query("select count(log.uid) from UserLog log where log.userLogType = 'USED_A_JOIN_CODE' and log.description = :groupUid ")
    int getUserLogsWithJoinCode(@Param("groupUid") String groupUid);
}
