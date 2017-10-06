package za.org.grassroot.messaging.service.sms.aat;


import org.dom4j.Element;
import za.org.grassroot.messaging.service.sms.SMSDeliveryStatus;
import za.org.grassroot.messaging.service.sms.SentSMSStatus;

import java.time.LocalDateTime;

public class AatSMSSendStatus implements SentSMSStatus {

    private String messageKey;

    private String phoneNumber;

    private String message;

    private LocalDateTime timeSent;

    boolean delivered;

    private LocalDateTime timeDelivered;

    private AatMsgStatus status;

    private String statusDescription;


    public AatSMSSendStatus(Element xml) {

        this.messageKey = xml.attributeValue("key");
        this.phoneNumber = xml.attributeValue("tonumber");
        this.message = xml.attributeValue("message");
        this.timeSent = LocalDateTime.parse(xml.attributeValue("message"));
        this.delivered = xml.attributeValue("delivered").equals("1");
        int statusCode = Integer.parseInt(xml.attributeValue("status"));
        this.status = AatMsgStatus.fromCode(statusCode);
        this.statusDescription = xml.attributeValue("statusdescription");

    }

    @Override
    public String getMessageKey() {
        return this.messageKey;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public LocalDateTime getTimeDelivered() {
        return timeDelivered;
    }

    @Override
    public SMSDeliveryStatus getDeliveryStatus() {
        return this.status.toSMSDeliveryStatus();
    }
}
