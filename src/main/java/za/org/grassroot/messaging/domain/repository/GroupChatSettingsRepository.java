package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.org.grassroot.messaging.domain.Group;
import za.org.grassroot.messaging.domain.User;
import za.org.grassroot.messaging.domain.GroupChatSettings;

import java.time.Instant;
import java.util.List;

/**
 * Created by luke on 2017/05/20.
 */
public interface GroupChatSettingsRepository extends JpaRepository<GroupChatSettings, Long> {

    GroupChatSettings findTopByUserAndGroupOrderByCreatedDateTimeDesc(User user, Group group);

    long countByGroupAndActiveTrue(Group group);

    List<GroupChatSettings> findByActiveFalseAndUserInitiatedFalseAndReactivationTimeBefore(Instant time);

    List<GroupChatSettings> findByGroupAndActiveTrueAndCanSendFalse(Group group);

}
