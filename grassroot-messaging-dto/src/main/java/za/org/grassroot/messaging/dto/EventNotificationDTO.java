package za.org.grassroot.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EventNotificationDTO extends MessageDTO {

    private String notificationUid;
    private long notificationCreationTime;
    private EventType eventType;
    private String eventUid;

    @JsonCreator
    public EventNotificationDTO(@JsonProperty("title") String title,
                                @JsonProperty("text") String text,
                                @JsonProperty("notificationUid") String notificationUid,
                                @JsonProperty("notificationCreationTime") long notificationCreationTime,
                                @JsonProperty("eventType") EventType eventType,
                                @JsonProperty("eventUid") String eventUid) {

        super(title, text);
        this.notificationCreationTime = notificationCreationTime;
        this.notificationUid = notificationUid;
        this.eventType = eventType;
        this.eventUid = eventUid;
    }

    public String getNotificationUid() {
        return notificationUid;
    }

    public long getNotificationCreationTime() {
        return notificationCreationTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getEventUid() {
        return eventUid;
    }
}
