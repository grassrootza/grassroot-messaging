package za.org.grassroot.messaging.service.sms;

/**
 * Created by luke on 2016/09/19.
 */
public interface SmsGatewayResponse {

    SmsResponseType getResponseType();

    boolean isSuccessful();

    Integer getErrorCode();

    String getMessageKey();

}
