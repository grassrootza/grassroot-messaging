package za.org.grassroot.messaging.service.gcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.GcmRegistration;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.enums.DeliveryRoute;
import za.org.grassroot.core.repository.GcmRegistrationRepository;
import za.org.grassroot.core.repository.UserRepository;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.util.PhoneNumberUtil;

import java.util.UUID;

/**
 * Restructured and slimmed down, using Smack extensions, etc
 */
@Service
@ConditionalOnProperty(value = "grassroot.gcm.enabled", havingValue = "true")
public class GcmXmppBrokerImpl implements GcmHandlingBroker {

    private static final Logger logger = LoggerFactory.getLogger(GcmXmppBrokerImpl.class);

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private final GcmRegistrationRepository gcmRegistrationRepository;
    private final MessageChannel gcmXmppOutboundChannel;

    @Autowired
    public GcmXmppBrokerImpl(@Qualifier("gcmObjectMapper") ObjectMapper objectMapper, @Qualifier("gcmXmppOutboundChannel") MessageChannel gcmXmppOutboundChannel,
                             UserRepository userRepository, NotificationBroker notificationBroker, GcmRegistrationRepository gcmRegistrationRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.gcmRegistrationRepository = gcmRegistrationRepository;
        this.gcmXmppOutboundChannel = gcmXmppOutboundChannel;
    }

    @Override
    public void sendGcmMessage(GcmPayload payload) {
        logger.info("sending GCM payload: {}", payload);
        gcmXmppOutboundChannel.send(buildGcmFromPayload(payload));
    }

    @Override
    public void sendGcmAcknowledgement(String registrationId, String messageId) {
        GcmPayload gcmPayload = new GcmPayload(messageId, registrationId, "ack");
        logger.debug("Acknowledging message with id ={}", messageId);
        gcmXmppOutboundChannel.send(buildGcmFromPayload(gcmPayload));
    }

    @Override
    @Transactional
    public void registerUser(String inputNumber, String registrationId) {
        String convertedNumber = PhoneNumberUtil.convertPhoneNumber(inputNumber);
        if (convertedNumber == null) {
            logger.warn("Error, received bad number, exiting registration");
            return;
        }

        User user = userRepository.findByPhoneNumberAndPhoneNumberNotNull(convertedNumber);
        if (user == null) {
            logger.warn("received valid phone number, but no user found", convertedNumber);
            return;
        }

        logger.debug("Registering user with phoneNumber = {} as a push notification recipient", convertedNumber);
        GcmRegistration gcmRegistration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(user);
        if (gcmRegistration != null) {
            gcmRegistration.setRegistrationId(registrationId);
        } else {
            gcmRegistrationRepository.save(new GcmRegistration(user, registrationId));
        }
        user.setMessagingPreference(DeliveryRoute.ANDROID_APP);
    }

    @Scheduled(fixedRate = 300000) // runs every five minutes
    public void gcmKeepAlive() {
        GcmPayload gcmPayload = new GcmPayload(UUID.randomUUID().toString(), "topics/keepalive", null);
        gcmXmppOutboundChannel.send(buildGcmFromPayload(gcmPayload));
    }

    private ObjectMapper mapper = new ObjectMapper();
    private Message<org.jivesoftware.smack.packet.Message> buildGcmFromPayload(GcmPayload gcmPayload) {
        try {
            org.jivesoftware.smack.packet.Message xmppMessage = new org.jivesoftware.smack.packet.Message();
            String payloadJson = mapper.writeValueAsString(gcmPayload);
            xmppMessage.addExtension(new GcmPacketExtension(payloadJson));
            return MessageBuilder.withPayload(xmppMessage).build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Badly formed GcmMessage passed as payload");
        }
    }

}