package za.org.grassroot.messaging.service.email;

import org.springframework.messaging.Message;
import za.org.grassroot.core.dto.GrassrootEmail;

import java.util.Set;

public interface EmailSendingBroker {

    void sendNotificationByEmail(Message message);

    void sendNonNotificationEmails(Set<GrassrootEmail> emails);

}
