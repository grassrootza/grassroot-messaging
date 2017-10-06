package za.org.grassroot.messaging.service.sms;

/**
 * Created by luke on 2015/09/09.
 */
public interface SmsSendingService {

    SmsGatewayResponse sendSMS(String message, String destinationNumber);

    SmsGatewayResponse sendPrioritySMS(String message, String destinationNumber);

}
