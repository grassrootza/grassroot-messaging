package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.org.grassroot.messaging.domain.Group;

/**
 * Created by luke on 2017/05/20.
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    Group findOneByUid(String uid);

}