package za.org.grassroot.messaging.service.sms;

/**
 * Created by luke on 2015/09/09.
 */
public interface SmsSendingService {

    SmsGatewayResponse sendSMS(String message, String destinationNumber, boolean longMessageAllowed);

    SmsGatewayResponse sendPrioritySMS(String message, String destinationNumber);

    SMSDeliveryReceipt fetchSMSDeliveryStatus(String messageKey) throws Exception;

}
