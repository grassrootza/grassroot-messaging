package za.org.grassroot.messaging.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.org.grassroot.messaging.domain.GcmRegistration;
import za.org.grassroot.messaging.domain.User;

/**
 * Ported by Luke on 2017/05/18.
 */
public interface GcmRegistrationRepository extends JpaRepository<GcmRegistration, Long> {

    GcmRegistration findTopByUserOrderByCreationTimeDesc(User user);

}