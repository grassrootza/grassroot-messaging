package za.org.grassroot.messaging.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by paballo on 2016/10/28.
 */

@Configuration
@ComponentScan
@IntegrationComponentScan
@EnableIntegration
@ConditionalOnProperty(name = "grassroot.mqtt.enabled", havingValue = "true",  matchIfMissing = false)
public class MQTTConfig {

    private static final Logger logger = LoggerFactory.getLogger(MQTTConfig.class);

    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat CHAT_TIME_J7FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${grassroot.mqtt.connection.url:tcp://localhost:1883}")
    private String host;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        logger.info("STARTUP: Mqtt host: {}", host);
        factory.setServerURIs(host);
        factory.setCleanSession(false);
        return factory;
    }

    @Bean
    public MessageProducerSupport messageProducerSupport(){
        return mqttAdapter();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttAdapter(){
        MqttPahoMessageDrivenChannelAdapter adapter
                = new MqttPahoMessageDrivenChannelAdapter("Grassroot",
                mqttClientFactory());
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setOutputChannel(mqttInboundChannel());
        adapter.setQos(1);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MqttPahoMessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler("Grassroot Server", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setConverter(new DefaultPahoMessageConverter());
        return messageHandler;
    }

    @Bean
    public MessageChannel mqttInboundChannel(){
        return  new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean("mqttObjectMapper")
    public ObjectMapper complexMqttPayloadMapper() {
        ObjectMapper payloadMapper = new ObjectMapper();
        payloadMapper.setDateFormat(CHAT_TIME_J7FORMAT);
        SimpleModule ldtModule = new SimpleModule();
        // we might be able to rewrite these as lambdas; but also, keep an eye on which bean uses which mapper
        ldtModule.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
                gen.writeString(value.format(CHAT_TIME_FORMAT));
            }
        }).addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return LocalDateTime.parse(p.getText(), CHAT_TIME_FORMAT);
            }
        });
        payloadMapper.registerModule(ldtModule);
        payloadMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        payloadMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        return payloadMapper;
    }

}