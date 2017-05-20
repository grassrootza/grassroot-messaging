package za.org.grassroot.messaging.domain.sms;

/**
 * Created by luke on 2016/09/19.
 */
public interface SmsGatewayResponse {

    SmsResponseType getResponseType();
    boolean isSuccessful();
    Integer getErrorCode();

}
