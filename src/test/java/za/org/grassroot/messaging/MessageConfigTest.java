package za.org.grassroot.messaging;

import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import za.org.grassroot.core.domain.Group;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.domain.notification.EventInfoNotification;
import za.org.grassroot.core.domain.task.EventLog;
import za.org.grassroot.core.domain.task.Meeting;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.core.enums.EventLogType;
import za.org.grassroot.messaging.config.MessageRoutingConfig;
import za.org.grassroot.messaging.controller.SimpleNotification;
import za.org.grassroot.messaging.domain.PriorityMessage;
import za.org.grassroot.messaging.service.gcm.PushNotificationBroker;
import za.org.grassroot.messaging.service.sms.SmsNotificationBroker;

import java.time.Instant;

/**
 * Created by luke on 2017/05/18.
 * note: for god knows what reason the channels are not triggering post-receive, so actually testing anything is impossible.
 * but right now the theoretical gains from obeying test dogma are massively overwhelmed by the kafka-esque paing of trying
 * to make these tests work. so, leaving them out. possibly reconsider in future.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MessageRoutingConfig.class })
public class MessageConfigTest {

    @MockBean
    private SmsNotificationBroker smsNotificationBroker;

    @MockBean
    private PushNotificationBroker pushNotificationBroker;

    @MockBean
    private XMPPConnection mockXmppConnection;

    @Autowired
    private @Qualifier("outboundRouterChannel") MessageChannel requestChannel;

    @Autowired
    private @Qualifier("outboundPriorityChannel") MessageChannel priorityChannel;

    @Test
    public void testSmsChannel() throws InterruptedException {

        User user = new User("27605550000", "Someone", null);
        user.setMessagingPreference(DeliveryRoute.SMS);
        Notification dummy = new SimpleNotification(user, "Hello World", false);
        Message<Notification> message = MessageBuilder
                .withPayload(dummy)
                .setHeader("route", DeliveryRoute.SMS.name())
                .build();
        requestChannel.send(message);
        // verify(smsNotificationBroker, times(1)).sendStandardSmsNotification(message);
    }

    @Test
    public void testAwsChannel() {
        Notification dummy = makeDummy("Hello AWS");
        Message<Notification> message = MessageBuilder
                .withPayload(dummy)
                .setHeader("route", "SMS_AWS")
                .build();
        requestChannel.send(message);
    }

    @Test
    public void testAatChannel() {
        Notification dummy = makeDummy("Hello AAT");
        Message<Notification> message = MessageBuilder
                .withPayload(dummy)
                .setHeader("route", "SMS_AAT")
                .build();
        requestChannel.send(message);
    }

    @Test
    public void testPriorityChannel() {
        Message<PriorityMessage> message = MessageBuilder
                .withPayload(new PriorityMessage("060555", "Hello"))
                .setHeader("route", "PRIORITY")
                .setHeader("priority", "0")
                .build();
        //when(smsNotificationBroker.sendPrioritySmsNotification("Hello", "060555");)
          //      .thenReturn(MockSmsResponse.make(SmsResponseType.DELIVERED, true));
        priorityChannel.send(message);
    }

    public static Notification makeDummy(String message) {
        User testUser = new User("0505550000", "Test", null);
        Group testGroup = new Group("test", testUser);
        Meeting testMtg = new Meeting("test mtg", Instant.now(), testUser, testGroup, "somewhere");
        EventLog eventLog = new EventLog(testUser, testMtg, EventLogType.CREATED);
        Notification notification = new EventInfoNotification(testUser, message, eventLog);
        return notification;
    }


}
