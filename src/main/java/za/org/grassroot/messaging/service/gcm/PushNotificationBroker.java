package za.org.grassroot.messaging.service.gcm;

import org.springframework.messaging.Message;

/**
 * Created by luke on 2017/05/19.
 */
public interface PushNotificationBroker {

    void sendMessage(Message message);

}
