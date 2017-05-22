package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
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

}
