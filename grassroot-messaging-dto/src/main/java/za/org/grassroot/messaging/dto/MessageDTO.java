package za.org.grassroot.messaging.dto;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = DefaultMessageDTO.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EventNotificationDTO.class, name = "eventNotification"),
        @JsonSubTypes.Type(value = DefaultMessageDTO.class, name = "defaultMessage"),
        @JsonSubTypes.Type(value = JoinRequestDTO.class, name = "joinReqNotification"),
        @JsonSubTypes.Type(value = UserLogDTO.class, name = "userLogNotification"),
})
public abstract class MessageDTO {

    private String title;

    private String text;


    @JsonCreator
    public MessageDTO(@JsonProperty("title") String title,
                      @JsonProperty("text") String text
    ) {

        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

}
