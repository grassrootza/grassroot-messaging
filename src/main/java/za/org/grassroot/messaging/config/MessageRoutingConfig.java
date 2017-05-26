package za.org.grassroot.messaging.config;

import org.jivesoftware.smack.XMPPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.xmpp.outbound.ChatMessageSendingMessageHandler;
import org.springframework.messaging.MessageChannel;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.service.gcm.PushNotificationBroker;
import za.org.grassroot.messaging.service.sms.SmsNotificationBroker;
import za.org.grassroot.messaging.service.sms.SmsSendingStrategy;

/**
 * Created by luke on 2017/05/18.
 */
@Configuration
@SuppressWarnings("SpringJavaAutowiringInspection")
public class MessageRoutingConfig {

    private static final Logger logger = LoggerFactory.getLogger(MessageRoutingConfig.class);

    private SmsNotificationBroker smsNotificationBroker;
    private PushNotificationBroker pushNotificationBroker;

    @Autowired
    public void setSmsNotificationBroker(SmsNotificationBroker smsNotificationBroker) {
        this.smsNotificationBroker = smsNotificationBroker;
    }

    @Autowired
    public void setPushNotificationBroker(PushNotificationBroker pushNotificationBroker) {
        this.pushNotificationBroker = pushNotificationBroker;
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller() {
        return Pollers
                .fixedRate(500)
                .get();
    }

    @Bean
    public MessageChannel outboundRouterChannel() {
        return MessageChannels.queue().get();
    }

    @Bean
    public MessageChannel gcmXmppOutboundChannel(){
        return new DirectChannel();
    }

    @Bean
    public MessageChannel outboundPriorityChannel() { return MessageChannels.priority().get(); }


    @Bean
    @ServiceActivator(inputChannel = "gcmXmppOutboundChannel")
    public ChatMessageSendingMessageHandler chatMessageSendingMessageHandler(XMPPConnection connection){
        return new ChatMessageSendingMessageHandler(connection);
    }

    @Bean
    public IntegrationFlow priorityFlow() {
        return f -> f.channel("outboundPriorityChannel")
                .handle(smsNotificationBroker::sendPrioritySmsNotification);
    }

    @Bean
    public IntegrationFlow routerFlow() {
        return f -> f.channel("outboundRouterChannel")
                .route(headerRouter());
    }

    @Bean
    public HeaderValueRouter headerRouter() {
        HeaderValueRouter router = new HeaderValueRouter("route");
        router.setDefaultOutputChannelName("smsDefaultOutboundChannel");
        router.setChannelMapping(UserMessagingPreference.SMS.name(), "smsDefaultOutboundChannel");
        router.setChannelMapping("SMS_AWS", "smsAwsOutboundChannel");
        router.setChannelMapping(UserMessagingPreference.ANDROID_APP.name(), "gcmOutboundChannel");
        router.setIgnoreSendFailures(true);
        router.setLoggingEnabled(true);
        router.setResolutionRequired(false); // will route to SMS
        return router;
    }

    @Bean
    public IntegrationFlow smsFlow() {
        return f -> f.channel("smsDefaultOutboundChannel")
                .log(LoggingHandler.Level.DEBUG)
                .handle(smsNotificationBroker::sendStandardSmsNotification);
    }

    @Bean
    public IntegrationFlow awsSmsFlow() {
        return f -> f.channel("smsAwsOutboundChannel")
                .handle(m -> smsNotificationBroker.sendSmsNotificationByStrategy(m, SmsSendingStrategy.AWS));
    }

    @Bean
    public IntegrationFlow aatSmsFlow() {
        return f -> f.channel("smsAatOutboundChannel")
                .handle(m -> smsNotificationBroker.sendSmsNotificationByStrategy(m, SmsSendingStrategy.AAT));
    }

    @Bean
    public IntegrationFlow gcmOutboundFlow() {
        return f -> f.channel("gcmOutboundChannel")
                .handle(pushNotificationBroker::sendMessage);
    }

}