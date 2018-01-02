package za.org.grassroot.messaging.service.sms.aat;

import lombok.extern.slf4j.Slf4j;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.sms.SmsResponseType;

/**
 * Created by luke on 2016/09/19.
 * utility class to turn the JAXB-interpreted AAT response into something more abstract and usable
 */
@Slf4j
public class AatResponseInterpreter implements SmsGatewayResponse {

    private static final String successAction = "enqueued";

    private SmsResponseType responseType;
    private boolean successful;
    private Integer aatErrorCode;
    private String messageKey = null;

    public static AatResponseInterpreter makeDummy() {
        AatResponseInterpreter response = new AatResponseInterpreter();
        response.responseType = SmsResponseType.DELIVERED;
        response.successful = true;
        response.aatErrorCode = null;
        return response;
    }

    public static AatResponseInterpreter makeErrorResponse(SmsResponseType responseType) {
        AatResponseInterpreter response = new AatResponseInterpreter();
        response.responseType = responseType;
        response.successful = false;
        response.aatErrorCode = null;
        return response;
    }

    private AatResponseInterpreter() {
        // for above
    }

    public AatResponseInterpreter(AatSmsResponse rawResponse) {

        if (rawResponse.getSubmitresult().getAction().equals(successAction)) {
            this.responseType = SmsResponseType.ROUTED;
            this.successful = true;
            this.messageKey = rawResponse.submitresult.key.toString();
        } else if (rawResponse.getSubmitresult().error != null) {
            this.successful = false;
            this.aatErrorCode = rawResponse.getSubmitresult().error;
            switch (aatErrorCode) {
                case 150:
                case 151:
                case 152:
                    this.responseType = SmsResponseType.INVALID_CREDENTIALS;
                    break;
                case 153:
                    this.responseType = SmsResponseType.INSUFFICIENT_FUNDS;
                    break;
                case 154:
                case 156:
                    this.responseType = SmsResponseType.MSISDN_INVALID;
                    break;
                case 155:
                    this.responseType = SmsResponseType.DUPLICATE_MESSAGE;
                    break;
                default:
                    this.responseType = SmsResponseType.UNKNOWN_ERROR;
                    break;
            }
        } else {
            this.successful = false;
            this.responseType = SmsResponseType.UNKNOWN_ERROR;
        }
    }

    @Override
    public SmsResponseType getResponseType() {
        return responseType;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }


    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public MessagingProvider getProvider() {
        return MessagingProvider.AAT;
    }


    @Override
    public String toString() {
        return "AatResponseInterpreter{" +
                "responseType=" + responseType +
                ", successful=" + successful +
                '}';
    }
}
