package za.org.grassroot.messaging.service.sms.aat;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import za.org.grassroot.messaging.service.sms.SMSDeliveryReceipt;
import za.org.grassroot.messaging.service.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.sms.SmsResponseType;
import za.org.grassroot.messaging.service.sms.SmsSendingService;

import java.io.StringReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;

/**
 * Created by luke on 2015/09/09.
 */
@Primary @Slf4j
@Service("aatSmsSender")
@PropertySource(value = "file:${grassroot.messaging.properties.path}", ignoreResourceNotFound = true)
public class AatSmsSendingManager implements SmsSendingService {

    private static final String ALLOWABLE_C_CODE = "27";

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
    public SmsGatewayResponse sendSMS(String message, String destinationNumber, boolean longMessageAllowed) {
        if (StringUtils.isEmpty(destinationNumber)) {
            log.error("Error! Called send SMS with null number, returning error");
            return AatResponseInterpreter.makeErrorResponse(SmsResponseType.COMMUNICATION_ERROR);
        }

        // we are going to skip international numbers
        if (!ALLOWABLE_C_CODE.equals(destinationNumber.substring(0, 2))) {
            log.info("skipping international number: {}, substring: {}", destinationNumber, destinationNumber.substring(0, 1));
            return AatResponseInterpreter.makeErrorResponse(SmsResponseType.INTL_NUMBER);
        }

        try {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.put("username", Collections.singletonList(smsGatewayUsername));
            queryParams.put("password", Collections.singletonList(smsGatewayPassword));
            queryParams.put("number", Collections.singletonList(destinationNumber));
            queryParams.put("message", Collections.singletonList(replaceSmartQuotes(message)));

            if (longMessageAllowed && message.length() > 160) {
                queryParams.put("ems", Collections.singletonList("1"));
            }

            long startTime = System.currentTimeMillis();
            UriComponentsBuilder gatewayURI = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host(smsGatewayHost)
                    .path("send/")
                    .queryParams(queryParams);

            log.info("Sending AAT SMS to {} ...", destinationNumber);
            URI encodedUri = gatewayURI.build().encode(Charset.forName("UTF-8")).toUri(); // let's see
            log.debug("AAT SMS url: {}", encodedUri);
            SmsGatewayResponse response = environment.acceptsProfiles("default") ? AatResponseInterpreter.makeDummy() :
                    new AatResponseInterpreter(restTemplate.getForObject(encodedUri, AatSmsResponse.class));
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
//        String messageEmojiStripped = EmojiParser.removeAllEmojis(message).replaceAll("[<=_]", "");
//        return messageEmojiStripped.replaceAll("\\s*&\\s*", " and ");
        return EmojiParser.removeAllEmojis(message);
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
