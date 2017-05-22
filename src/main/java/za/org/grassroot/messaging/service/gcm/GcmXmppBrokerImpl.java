package za.org.grassroot.messaging.service.gcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.GcmRegistration;
import za.org.grassroot.messaging.domain.repository.GcmRegistrationRepository;
import za.org.grassroot.messaging.domain.User;
import za.org.grassroot.messaging.domain.repository.UserRepository;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;
import za.org.grassroot.messaging.util.PhoneNumberUtil;

import java.util.UUID;

/**
 * Restructured and slimmed down, using Smack extensions, etc
 */
@Service
public class GcmXmppBrokerImpl implements GcmHandlingBroker {

    private static final Logger logger = LoggerFactory.getLogger(GcmXmppBrokerImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final UserRepository userRepository;
    private final GcmRegistrationRepository gcmRegistrationRepository;

    private final MessageChannel gcmXmppOutboundChannel;

    static {
        SmackConfiguration.DEBUG = false;
    }

    @Autowired
    public GcmXmppBrokerImpl(UserRepository userRepository, GcmRegistrationRepository gcmRegistrationRepository,
                             @Qualifier("gcmXmppOutboundChannel") MessageChannel gcmXmppOutboundChannel) {
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

        User user = userRepository.findOneByPhoneNumber(convertedNumber);
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
        user.setMessagingPreference(UserMessagingPreference.ANDROID_APP);
    }

    @Scheduled(fixedRate = 300000) // runs every five minutes
    public void gcmKeepAlive() {
        GcmPayload gcmPayload = new GcmPayload(UUID.randomUUID().toString(), "topics/keepalive", null);
        gcmXmppOutboundChannel.send(buildGcmFromPayload(gcmPayload));
    }

    private Message<org.jivesoftware.smack.packet.Message> buildGcmFromPayload(GcmPayload gcmPayload) {
        try {
            org.jivesoftware.smack.packet.Message xmppMessage = new org.jivesoftware.smack.packet.Message();
            xmppMessage.addExtension(new GcmPacketExtension(objectMapper.writeValueAsString(gcmPayload)));
            return MessageBuilder.withPayload(xmppMessage).build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Badly formed GcmMessage passed as payload");
        }
    }

}