package za.org.grassroot.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserLogDTO extends MessageDTO {

    private String notificationUid;
    private long notificationCreationTime;
    private String userLogType;

    @JsonCreator
    public UserLogDTO(@JsonProperty("title") String title,
                      @JsonProperty("text") String text,
                      @JsonProperty("notificationUid") String notificationUid,
                      @JsonProperty("notificationCreationTime") long notificationCreationTime,
                      @JsonProperty("userLogType") String userLogType) {

        super(title, text);
        this.notificationUid = notificationUid;
        this.notificationCreationTime = notificationCreationTime;
        this.userLogType = userLogType;

    }

    public String getNotificationUid() {
        return notificationUid;
    }

    public long getNotificationCreationTime() {
        return notificationCreationTime;
    }

    public String getUserLogType() {
        return userLogType;
    }
}
