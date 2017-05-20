package za.org.grassroot.messaging.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import za.org.grassroot.messaging.domain.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.domain.sms.AatResponseInterpreter;
import za.org.grassroot.messaging.domain.sms.AatSmsResponse;

/**
 * Created by luke on 2015/09/09.
 */
@Primary
@Service("aatSmsSender")
@PropertySource(value = "file:${user.home}/grassroot/grassroot-integration.properties", ignoreResourceNotFound = true)
public class AatSmsSendingManager implements SmsSendingService {

    private Logger log = LoggerFactory.getLogger(AatSmsSendingManager.class);

    private final Environment environment;
    private final RestTemplate restTemplate;

    @Value("${grassroot.sms.gateway}")
    private String smsGatewayHost;
    @Value("${grassroot.sms.gateway.username}")
    private String smsGatewayUsername;
    @Value("${grassroot.sms.gateway.password}")
    private String smsGatewayPassword;
    @Value("${grassroot.sms.priority.username}")
    private String smsPriorityUsername;
    @Value("${grassroot.sms.priority.password}")
    private String smsPriorityPassword;

    @Autowired
    public AatSmsSendingManager(Environment environment, RestTemplate restTemplate) {
        this.environment = environment;
        this.restTemplate = restTemplate;
    }

    @Override
    public SmsGatewayResponse sendSMS(String message, String destinationNumber) {
        UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance().scheme("https").host(smsGatewayHost);
        gatewayURI.path("send/").queryParam("username", smsGatewayUsername)
                .queryParam("password", smsGatewayPassword)
                .queryParam("number", destinationNumber)
                .queryParam("message", message);
        return environment.acceptsProfiles("default") ? AatResponseInterpreter.makeDummy() :
            new AatResponseInterpreter(restTemplate.getForObject(gatewayURI.build().toUri(), AatSmsResponse.class));
    }

    @Override
    public SmsGatewayResponse sendPrioritySMS(String message, String destinationNumber) {
        UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance().scheme("https").host(smsGatewayHost);
        gatewayURI.path("send/")
                .queryParam("username", smsPriorityUsername)
                .queryParam("password", smsPriorityPassword)
                .queryParam("number", destinationNumber)
                .queryParam("message", message);
        return new AatResponseInterpreter(restTemplate.getForObject(gatewayURI.build().toUri(), AatSmsResponse.class));
    }

}
