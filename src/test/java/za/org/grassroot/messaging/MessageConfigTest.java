package za.org.grassroot.messaging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;

/**
 * Created by luke on 2017/05/18.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "grassroot.sms.gateway=test.nothing",
        "grassroot.sms.gateway.username=grassrootstest",
        "grassroot.sms.gateway.password=12345",
        "grassroot.sms.priority.username=grassrootstest",
        "grassroot.sms.priority.password=12345"
}, classes = Application.class)
public class MessageConfigTest {

    @Autowired
    private @Qualifier("outboundRouterChannel") MessageChannel requestChannel;

    @Test
    public void testSmsChannel() {
        Notification dummy = Notification.makeDummy("Hello World");
        Message<Notification> message = MessageBuilder
                .withPayload(dummy)
                .setHeader("route", UserMessagingPreference.SMS.name())
                .build();
        requestChannel.send(message);
    }

    @Test
    public void testAwsChannel() {
        Notification dummy = Notification.makeDummy("Hello AWS");
        Message<Notification> message = MessageBuilder
                .withPayload(dummy)
                .setHeader("route", "SMS_AWS")
                .build();
        requestChannel.send(message);
    }

    @Test
    public void testAatChannel() {
        Notification dummy = Notification.makeDummy("Hello AAT");
        Message<Notification> message = MessageBuilder
                .withPayload(dummy)
                .setHeader("route", "SMS_AAT")
                .build();
        requestChannel.send(message);
    }

}
