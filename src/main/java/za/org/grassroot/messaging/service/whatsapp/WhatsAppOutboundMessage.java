package za.org.grassroot.messaging.service.whatsapp;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class WhatsAppOutboundMessage {

    private WhatsAppOutboundMessageType type;

    private String recipient;
    private String namespace;
    private String elementName;
    private String language;

    private List<WhatsAppMessageParameter> parameters = new ArrayList<>();

    public void addParameter(WhatsAppMessageParameter parameter) {
        this.parameters.add(parameter);
    }

}
