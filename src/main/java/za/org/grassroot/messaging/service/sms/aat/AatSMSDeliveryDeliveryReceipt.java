package za.org.grassroot.messaging.service.sms.aat;


import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import za.org.grassroot.messaging.service.sms.SMSDeliveryReceipt;
import za.org.grassroot.messaging.service.sms.SMSDeliveryStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AatSMSDeliveryDeliveryReceipt implements SMSDeliveryReceipt {

    private String messageKey;

    private String phoneNumber;

    private String message;

    boolean delivered;

    private LocalDateTime timeDelivered;

    private AatMsgStatus status;

    private String statusDescription;


    public AatSMSDeliveryDeliveryReceipt(Element xml) {

        this.messageKey = xml.getAttributeValue("key");
        this.phoneNumber = xml.getAttributeValue("tonumber");
        this.message = xml.getAttributeValue("message");
        this.timeDelivered = LocalDateTime.parse(xml.getAttributeValue("timedelivered"), DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
        this.delivered = xml.getAttributeValue("delivered").equals("1");
        int statusCode = Integer.parseInt(xml.getAttributeValue("status"));
        this.status = AatMsgStatus.fromCode(statusCode);
        if (status == null)
            log.error("Could not resolve aat message status from code : " + statusCode);
        this.statusDescription = xml.getAttributeValue("statusdescription");

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
    public String getDescription() {
        return this.statusDescription;
    }

    @Override
    public SMSDeliveryStatus getDeliveryStatus() {
        return this.status.toSMSDeliveryStatus();
    }
}
