package za.org.grassroot.messaging.config;

import org.jivesoftware.smack.XMPPConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.xmpp.outbound.ChatMessageSendingMessageHandler;
import org.springframework.messaging.MessageChannel;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.messaging.service.email.EmailSendingBroker;
import za.org.grassroot.messaging.service.gcm.PushNotificationBroker;
import za.org.grassroot.messaging.service.sms.SmsNotificationBroker;
import za.org.grassroot.messaging.service.sms.SmsSendingStrategy;

/**
 * Created by luke on 2017/05/18.
 */
@Configuration
@SuppressWarnings("SpringJavaAutowiringInspection")
public class MessageRoutingConfig {

    private SmsNotificationBroker smsNotificationBroker;
    private PushNotificationBroker pushNotificationBroker;
    private EmailSendingBroker emailSendingBroker;

    @Autowired
    public void setSmsNotificationBroker(SmsNotificationBroker smsNotificationBroker) {
        this.smsNotificationBroker = smsNotificationBroker;
    }

    @Autowired(required = false)
    public void setPushNotificationBroker(PushNotificationBroker pushNotificationBroker) {
        this.pushNotificationBroker = pushNotificationBroker;
    }

    @Autowired(required = false)
    public void setEmailSendingBroker(EmailSendingBroker emailSendingBroker) {
        this.emailSendingBroker = emailSendingBroker;
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
    public MessageChannel outboundSystemChannel() {
        return MessageChannels.queue().get();
    }

    @Bean
    @ConditionalOnProperty(value = "grassroot.gcm.enabled", havingValue = "true")
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
    public IntegrationFlow pushSmsFlow() {
        return f -> f.channel("outboundSystemChannel")
                .handle(smsNotificationBroker::sendSmsWithoutNotification);
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
        router.setChannelMapping(DeliveryRoute.SMS.name(), "smsDefaultOutboundChannel");
        router.setChannelMapping("SMS_AWS", "smsAwsOutboundChannel");
        router.setChannelMapping(DeliveryRoute.ANDROID_APP.name(), "gcmOutboundChannel");
        router.setChannelMapping(DeliveryRoute.EMAIL_GRASSROOT.name(), "emailNotificationChannel");
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
    @ConditionalOnProperty(value = "grassroot.gcm.enabled", havingValue = "true")
    public IntegrationFlow gcmOutboundFlow() {
        return f -> f.channel("gcmOutboundChannel")
                .handle(pushNotificationBroker::sendMessage);
    }

    @Bean
    @ConditionalOnProperty(value = "grassroot.email.enabled", havingValue = "true")
    public IntegrationFlow emailNotificationFlow() {
        return f -> f.channel("emailNotificationChannel")
                .handle(emailSendingBroker::sendNotificationByEmail);
    }

}