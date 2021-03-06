package za.org.grassroot.messaging.service.sms.aat;

import za.org.grassroot.messaging.service.sms.SMSDeliveryStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum AatMsgStatus {


    WAITING_FOR_RECEIPT(-1),
    EN_ROUTE(1),
    DELIVERED(0, 2),
    EXPIRED(3),
    DELETED_BY_SMSC(4),
    CANNOT_DELIVER(5),
    ACCEPTED_FOR_DELIVERY(6),
    UNKNOWN_ERROR(7, 31),
    REJECTED_BY_SMSC(8),
    ABANDONED(10),
    DELETED_BY_OPERATOR(11),
    WAITING_TO_BE_QUEUED(12),
    WAITING_AT_SMSC(13),
    WAITING_FOR_RESEND(14),
    DESTINATION_OUT_OF_RANGE(15),
    DESTINATION_CONGESTED(16),
    DESTINATION_INVALID(17),
    REFUSED_BY_DESTINATION(18),
    DESTINATION_SIM_REFUSED(19),
    SMSC_ERROR(20),
    PROVIDER_ERROR(21),
    DESTINATION_NOT_ON_NETWORK(22),
    INVALID_MESSAGE(23),
    NETWORK_ERROR(24),
    NO_ROUTE(25),
    CONGESTED_WILL_RETRY(26),
    BUSY_WILL_RETRY(27),
    NO_RESPONSE_WILL_RETRY(28),
    REJECTED_WILL_RETRY(29),
    LOW_SIGNAL_WILL_RETRY(30),

    INVALID_USERNAME_PASSWORD(150),
    LOGS_DISABLED(151),
    SENDING_DISABLED(152),
    NOT_ENOUGH_CREDITS(153),
    INVALID_NUMBER(154),
    DUPLICATE_MESSAGE(155),
    INVALID_NUMBER_NO_ROUTE(156),
    GATEWAY_DNS_NOT_FOUND(157),
    NO_SUBSCRIPTION_TO_XML_2_SMS(158),
    INVALID_PATH(159),
    PROBLEM_COMMUNICATING_WITH_GATEWAY(160),
    INVALID_QUERY_STRING(161);

    private Set<Integer> codes;

    private static List<AatMsgStatus> deliveryInProgressStatuses = Arrays.asList(EN_ROUTE, WAITING_FOR_RECEIPT, ACCEPTED_FOR_DELIVERY, WAITING_TO_BE_QUEUED, WAITING_AT_SMSC, WAITING_FOR_RESEND,
            CONGESTED_WILL_RETRY, BUSY_WILL_RETRY, NO_RESPONSE_WILL_RETRY, REJECTED_WILL_RETRY, LOW_SIGNAL_WILL_RETRY);

    private static List<AatMsgStatus> invalidNumberStatuses = Arrays.asList(
            INVALID_NUMBER_NO_ROUTE, INVALID_NUMBER, CANNOT_DELIVER, DESTINATION_INVALID
    );


    AatMsgStatus(Integer... code) {
        this.codes = new HashSet<>();
        for (Integer c : code) {
            codes.add(c);
        }
    }

    public static AatMsgStatus fromCode(Integer code) {
        for (int i = 0; i < values().length; i++) {
            AatMsgStatus statusCode = values()[i];
            if (statusCode.codes.contains(code))
                return statusCode;
        }
        return null;
    }


    public SMSDeliveryStatus toSMSDeliveryStatus() {
        if (this == DELIVERED)
            return SMSDeliveryStatus.DELIVERED;
        else if (deliveryInProgressStatuses.contains(this))
            return SMSDeliveryStatus.DELIVERY_IN_PROGRESS;
        else if (invalidNumberStatuses.contains(this))
            return SMSDeliveryStatus.PROBLEM_NUMBER;
        else
            return SMSDeliveryStatus.DELIVERY_FAILED;
    }
}
