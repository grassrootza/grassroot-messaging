package za.org.grassroot.messaging.service.whatsapp;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.enums.NotificationDetailedType;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service @Slf4j
@ConditionalOnProperty(value = "grassroot.whatsapp.enabled", havingValue = "true")
public class WhatsAppBrokerImpl implements WhatsAppBroker {

    @Value("${grassroot.whatsapp.outbound.queue:outbound-queue-dummy}")
    private String callbackQueueName;

    @Value("${grassroot.whatsapp.outbound.namespace:outbound-namespace}")
    private String outboundNamespace;

    private AmazonSQS sqsClient;
    private ObjectMapper objectMapper;

    private Map<NotificationDetailedType, String> TEMPLATE_MAP = ImmutableMap.of(
            NotificationDetailedType.MEETING_CALLED, "meeting_called",
            NotificationDetailedType.VOTE_CALLED, "vote_called");

    @PostConstruct
    public void init() {
        this.sqsClient = AmazonSQSClientBuilder
                .standard()
                .withRegion(Regions.EU_WEST_1)
                .build();
    }

    @Autowired
    public void setObjectMapper(@Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendWhatsAppMessage(Notification notification) {
        WhatsAppOutboundMessage message = new WhatsAppOutboundMessage();
        message.setNamespace(outboundNamespace);

        message.setRecipient(notification.getTarget().getPhoneNumber());
        message.setElementName(TEMPLATE_MAP.getOrDefault(notification.getNotificationDetailedType(), "default_template"));
        message.setLanguage(notification.getTarget().getLanguageCode());
        message.setType(WhatsAppOutboundMessageType.TEMPLATE);

        SendMessageRequest request = new SendMessageRequest();
        try {
            request.setQueueUrl(callbackQueueName);
            request.withMessageBody(objectMapper.writeValueAsString(message));
            this.sqsClient.sendMessage(request);
            log.info("Sent message!");
        } catch (JsonProcessingException e) {
            log.error("Jackson sprang a leak. Error: ", e);
        } catch (AmazonSQSException e) {
            log.error("SQS sprang a leak. Error: ", e);
        }
    }

    @Override
    public void sendPriorityWhatsAppMessage(Notification notification) {

    }
}
