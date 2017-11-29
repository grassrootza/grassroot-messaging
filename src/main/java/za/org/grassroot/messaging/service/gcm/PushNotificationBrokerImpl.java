package za.org.grassroot.messaging.service.gcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.GcmRegistration;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.repository.GcmRegistrationRepository;
import za.org.grassroot.messaging.dto.MessageDTO;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.util.DebugUtil;

/**
 * Created by luke on 2017/05/19.
 */
@Service
@ConditionalOnProperty(value = "grassroot.gcm.enabled", havingValue = "true")
public class PushNotificationBrokerImpl implements PushNotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationBrokerImpl.class);

    private final GcmHandlingBroker sendingService;

    private final ObjectMapper objectMapper;

    private final GcmRegistrationRepository gcmRegistrationRepository;

    private final NotificationBroker notificationBroker;

    @Autowired
    public PushNotificationBrokerImpl(GcmHandlingBroker sendingService, @Qualifier("gcmObjectMapper") ObjectMapper objectMapper,
                                      GcmRegistrationRepository gcmRegistrationRepository, NotificationBroker notificationBroker) {
        this.sendingService = sendingService;
        this.objectMapper = objectMapper;
        this.gcmRegistrationRepository = gcmRegistrationRepository;
        this.notificationBroker = notificationBroker;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendMessage(Message message) {
        logger.info("sending message via GCM sender ...");
        Notification notification = (Notification) message.getPayload();
        GcmPayload payload = buildGcmFromMessagePayload(notification);
        sendingService.sendGcmMessage(payload);
        notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENT, null, true,
                notification.getUid(), MessagingProvider.GCM);

    }

    private GcmPayload buildGcmFromMessagePayload(Notification notification) {
        DebugUtil.transactionRequired(GcmXmppBrokerImpl.class);
        GcmRegistration registration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(notification.getTarget());
        MessageDTO data = GCMDataFactory.createGCMDTO(notification);
        String dataJson = "";
        try {
            dataJson = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new GcmPayload(notification.getUid(),
                registration.getRegistrationId(),
                GCMDataFactory.createCollapseKey(notification),
                dataJson);
    }


}
