package za.org.grassroot.messaging.service.sms;

import org.springframework.messaging.Message;
import za.org.grassroot.messaging.domain.Notification;

/**
 * Created by luke on 2017/05/18.
 */
public interface SmsNotificationBroker {

    void sendStandardSmsNotification(Message message);

    void sendSmsNotificationByStrategy(Message message, SmsSendingStrategy strategy);

    void sendPrioritySmsNotification(Message message);

    void sendSmsNotificationOnError(Message message);

}
