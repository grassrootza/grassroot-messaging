package za.org.grassroot.messaging.service.sms;

/**
 * Created by luke on 2016/09/19.
 */
public enum SmsResponseType {

    ROUTED,
    DELIVERED,
    INVALID_CREDENTIALS,
    INSUFFICIENT_FUNDS,
    DUPLICATE_MESSAGE,
    MSISDN_INVALID,
    INVALID_QUERY,
    UNKNOWN_ERROR,
    COMMUNICATION_ERROR,
    INTL_NUMBER

}
