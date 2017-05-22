package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.org.grassroot.messaging.domain.GroupChatMessageStats;

/**
 * Created by luke on 2017/05/20.
 */
public interface GroupChatStatsRepository extends JpaRepository<GroupChatMessageStats, Long> {

    GroupChatMessageStats findByMessageUidAndReadFalse(String messageUid);



}
