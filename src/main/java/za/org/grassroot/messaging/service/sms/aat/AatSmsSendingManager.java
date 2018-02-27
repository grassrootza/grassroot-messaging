package za.org.grassroot.messaging.service.sms.aat;

import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import za.org.grassroot.messaging.service.sms.SMSDeliveryReceipt;
import za.org.grassroot.messaging.service.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.sms.SmsResponseType;
import za.org.grassroot.messaging.service.sms.SmsSendingService;

import java.io.StringReader;

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
        String msgToSend = replaceSmartQuotes(replaceIllegalChars(message));
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
            log.error("Error invoking AAT!", e);
            SmsResponseType errorType = e instanceof RestClientException ?
                    SmsResponseType.COMMUNICATION_ERROR : SmsResponseType.UNKNOWN_ERROR;
            return AatResponseInterpreter.makeErrorResponse(errorType);
        }
    }


    public SMSDeliveryReceipt fetchSMSDeliveryStatus(String messageKey) throws Exception {

        UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance().scheme("https").host(smsGatewayHost);
        gatewayURI.path("sendlog/")
                .queryParam("username", smsGatewayUsername)
                .queryParam("password", smsGatewayPassword)
                .queryParam("key", messageKey);

        String response = restTemplate.getForEntity(gatewayURI.build().toUri(), String.class).getBody();
        response = response.replace("&", "");
        Document doc = new org.jdom2.input.SAXBuilder().build(new StringReader(response));

        Element rootEl = doc.getRootElement();
        Element msgEl = rootEl.getChild("message");

        if (msgEl != null) {
            return new AatSMSDeliveryDeliveryReceipt(msgEl);
        } else {
            return null;
        }
    }

    private String replaceIllegalChars(String message) {
        String messageEmojiStripped = EmojiParser.removeAllEmojis(message).replaceAll("[<=_]", "");
        return messageEmojiStripped.replaceAll("\\s*&\\s*", " and ");
    }

    // these are from Google base (couldn't find them in guava, so hand ported)
    private String replaceSmartQuotes(String str) {
        log.debug("special chars? : {}", indexOfChars(str, "\u0091\u0092\u2018\u2019", 0));
        str = replaceChars(str, "\u0091\u0092\u2018\u2019", '\'');
        str = replaceChars(str, "\u0093\u0094\u201c\u201d", '"');
        return str;
    }

    public static String replaceChars(String str, String oldchars,
                                      char newchar) {
        int pos = indexOfChars(str, oldchars, 0);
        if (pos == -1) {
            return str;
        }

        StringBuilder buf = new StringBuilder(str);
        do {
            buf.setCharAt(pos, newchar);
            pos = indexOfChars(str, oldchars, pos + 1);
        } while (pos != -1);

        return buf.toString();
    }

    public static int indexOfChars(String str, String chars, int fromIndex) {
        final int len = str.length();

        for (int pos = fromIndex; pos < len; pos++) {
            if (chars.indexOf(str.charAt(pos)) >= 0) {
                return pos;
            }
        }
        return -1;
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
