package za.org.grassroot.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultMessageDTO extends MessageDTO {

    @JsonCreator
    public DefaultMessageDTO(@JsonProperty("title") String title,
                             @JsonProperty("text") String text) {
        super(title, text);
    }
}
