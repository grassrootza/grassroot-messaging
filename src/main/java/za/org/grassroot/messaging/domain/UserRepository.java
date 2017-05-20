package za.org.grassroot.messaging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by luke on 2017/05/19.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    User findOneByPhoneNumber(String phoneNumber);
}