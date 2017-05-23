package za.org.grassroot.messaging.service.mqtt;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.stereotype.Component;

/**
 * Created by paballo on 2016/11/14.
 */
@Component
@ConditionalOnProperty(name = "grassroot.mqtt.enabled", havingValue = "true",  matchIfMissing = false)
public class ShutDownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger  = Logger.getLogger(ShutDownListener.class);

    private final MqttPahoMessageDrivenChannelAdapter mqttAdapter;
    private final MqttPahoMessageHandler mqttOutbound;

    @Autowired
    public ShutDownListener(MqttPahoMessageDrivenChannelAdapter mqttAdapter, MqttPahoMessageHandler mqttOutbound) {
        this.mqttAdapter = mqttAdapter;
        this.mqttOutbound = mqttOutbound;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("ContextClosedEvent");
        if(mqttAdapter.isRunning()) {
            logger.info("Closing inbound mqtt connection");
            mqttAdapter.stop();
        }
        if(mqttOutbound.isRunning()){
            logger.info("Closing outbound mqtt connection");
            mqttOutbound.stop();
        }
    }
}
