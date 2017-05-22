package za.org.grassroot.messaging.service.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by paballo on 2016/11/04.
 */
@Component
@ConditionalOnProperty(name = "mqtt.connection.enabled", havingValue = "true",  matchIfMissing = false)
public class InboundMqttMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(InboundMqttMessageHandler.class);

    private final GroupChatService groupChatService;
    private ObjectMapper payloadMapper;

    @Autowired
    public InboundMqttMessageHandler(GroupChatService groupChatService) {
        this.groupChatService = groupChatService;
    }

    @Autowired
    public void setPayloadMapper(@Qualifier("mqttObjectMapper") ObjectMapper payloadMapper) {
        this.payloadMapper = payloadMapper;
    }

    // todo : can probably turn into an integration flow
    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler mqttInboundMessageHandler() {
        return message -> {
            try {
                logger.info("incoming payload " + message.getPayload().toString());
                MQTTPayload payload = payloadMapper.readValue(message.getPayload().toString(), MQTTPayload.class);
                String topic = String.valueOf(message.getHeaders().get(MqttHeaders.TOPIC));
                if (topic.equals("Grassroot")) {
                    groupChatService.processCommandMessage(payload);
                } else {
                    groupChatService.createGroupChatMessageStats(payload);
                }
            } catch (IOException e) {
                logger.info("Error receiving message over mqtt");
                e.printStackTrace();
            }

        };
    }

}
