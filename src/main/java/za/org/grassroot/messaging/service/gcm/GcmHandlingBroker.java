package za.org.grassroot.messaging.service.gcm;

import org.springframework.messaging.Message;

/**
 * Created by luke on 2017/05/19.
 */
public interface GcmHandlingBroker {

    void sendGcmMessage(GcmPayload payload);

    void sendGcmAcknowledgement(String registrationId, String messageId);

    void registerUser(String inputNumber, String registrationId);

}
