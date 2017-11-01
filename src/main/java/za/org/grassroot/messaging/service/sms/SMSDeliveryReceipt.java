package za.org.grassroot.messaging.service.sms;


import java.time.LocalDateTime;

public interface SMSDeliveryReceipt {

    String getMessageKey();

    SMSDeliveryStatus getDeliveryStatus();

    String getPhoneNumber();

    boolean isDelivered();

    LocalDateTime getTimeDelivered();

    String getDescription();


}
