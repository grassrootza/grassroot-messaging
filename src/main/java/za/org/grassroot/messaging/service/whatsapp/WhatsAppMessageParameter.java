package za.org.grassroot.messaging.service.whatsapp;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
public class WhatsAppMessageParameter {

    private String defaultText;
    private Instant dateTime;

}
