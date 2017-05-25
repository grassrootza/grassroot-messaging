package za.org.grassroot.messaging.service.sms;

import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;

/**
 * Created by luke on 2017/05/18.
 */
public interface SmsNotificationBroker {

    @Async
    void sendStandardSmsNotification(Message message);

    @Async
    void sendSmsNotificationByStrategy(Message message, SmsSendingStrategy strategy);

    void sendPrioritySmsNotification(Message message);

    void sendSmsNotificationOnError(Message message);

}
