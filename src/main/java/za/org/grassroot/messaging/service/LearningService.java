package za.org.grassroot.messaging.service;

import za.org.grassroot.messaging.domain.exception.SeloParseDateTimeFailure;

import java.time.LocalDateTime;

/**
 * Created by shakka on 8/15/16.
 */
public interface LearningService {

   LocalDateTime parse(String phrase) throws SeloParseDateTimeFailure;

}
