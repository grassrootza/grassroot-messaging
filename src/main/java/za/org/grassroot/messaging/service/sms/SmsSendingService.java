package za.org.grassroot.messaging.service.sms;

import za.org.grassroot.messaging.service.sms.model.SmsGatewayResponse;

/**
 * Created by luke on 2015/09/09.
 */
public interface SmsSendingService {

    SmsGatewayResponse sendSMS(String message, String destinationNumber);

    SmsGatewayResponse sendPrioritySMS(String message, String destinationNumber);

}
