package za.org.grassroot.messaging.service.sms.aat;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
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
import za.org.grassroot.messaging.service.sms.SMSSendLog;
import za.org.grassroot.messaging.service.sms.SentSMSStatus;
import za.org.grassroot.messaging.service.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.sms.SmsSendingService;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 2015/09/09.
 */
@Primary
@Service("aatSmsSender")
@PropertySource(value = "file:${grassroot.messaging.properties.path}", ignoreResourceNotFound = true)
public class AatSmsSendingManager implements SmsSendingService {

    private Logger log = LoggerFactory.getLogger(AatSmsSendingManager.class);

    private final Environment environment;
    private final RestTemplate restTemplate;

    @Value("${grassroot.sms.gateway:gateway}")
    private String smsGatewayHost;
    @Value("${grassroot.sms.gateway.username:grassroottest}")
    private String smsGatewayUsername;
    @Value("${grassroot.sms.gateway.password:apassword}")
    private String smsGatewayPassword;
    @Value("${grassroot.sms.priority.username:grassroottest2}")
    private String smsPriorityUsername;
    @Value("${grassroot.sms.priority.password:anotherpassword}")
    private String smsPriorityPassword;

    @Autowired
    public AatSmsSendingManager(Environment environment, RestTemplate restTemplate) {
        this.environment = environment;
        this.restTemplate = restTemplate;
    }

    @Override
    public SmsGatewayResponse sendSMS(String message, String destinationNumber) {
        long startTime = System.currentTimeMillis();
        UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance().scheme("https").host(smsGatewayHost);
        String msgToSend = replaceIllegalChars(message);
        gatewayURI.path("send/").queryParam("username", smsGatewayUsername)
                .queryParam("password", smsGatewayPassword)
                .queryParam("number", destinationNumber)
                .queryParam("message", msgToSend);
        try {
            log.info("Sending AAT SMS to {} ...", destinationNumber);
            SmsGatewayResponse response = environment.acceptsProfiles("default") ? AatResponseInterpreter.makeDummy() :
                    new AatResponseInterpreter(restTemplate.getForObject(gatewayURI.build().toUri(), AatSmsResponse.class));
            log.debug("time to execute AAT sms: {} msecs", System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.error("Error invoking AAT! : {}", e.getMessage());
            e.printStackTrace();
            return AatResponseInterpreter.makeDummy();
        }
    }


    public SMSSendLog fetchSendLog(LocalDate startDate, LocalDate endDate) throws Exception {

        UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance().scheme("https").host(smsGatewayHost);
        gatewayURI.path("sendlog/")
                .queryParam("username", smsGatewayUsername)
                .queryParam("password", smsGatewayPassword)
                .queryParam("startdate", DateTimeFormatter.ofPattern("YYYYMMDD").format(startDate))
                .queryParam("enddate", DateTimeFormatter.ofPattern("YYYYMMDD").format(endDate));

        String response = restTemplate.getForEntity(gatewayURI.build().toUri(), String.class).getBody();
        Document doc = new SAXReader().read(new StringReader(response));

        List<SentSMSStatus> messageStatuses = new ArrayList<>();
        for (Object msgObj : doc.getRootElement().elements("message")) {
            Element msgEl = (Element) msgObj;
            SentSMSStatus smsStatus = new AatSMSSendStatus(msgEl);
            messageStatuses.add(smsStatus);
        }

        return new SMSSendLog(messageStatuses);

    }


    private String replaceIllegalChars(String message) {
        return message.replaceAll("\\s*&\\s*", " and ");
    }

    @Override
    public SmsGatewayResponse sendPrioritySMS(String message, String destinationNumber) {
        log.info("Sending a priority SMS inside AAT sender");
        UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance().scheme("https").host(smsGatewayHost);
        gatewayURI.path("send/")
                .queryParam("username", smsPriorityUsername)
                .queryParam("password", smsPriorityPassword)
                .queryParam("number", destinationNumber)
                .queryParam("message", replaceIllegalChars(message));
        return new AatResponseInterpreter(restTemplate.getForObject(gatewayURI.build().toUri(), AatSmsResponse.class));
    }

}
