package za.org.grassroot.messaging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ported by Luke on 2017/05/18.
 */
public interface GcmRegistrationRepository extends JpaRepository<GcmRegistration, Long> {

    GcmRegistration findTopByUserOrderByCreationTimeDesc(User user);

}
