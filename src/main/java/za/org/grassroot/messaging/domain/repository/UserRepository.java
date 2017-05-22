package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.org.grassroot.messaging.domain.User;

/**
 * Created by luke on 2017/05/19.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    User findOneByUid(String uid);

    User findOneByPhoneNumber(String phoneNumber);
}