package za.org.grassroot.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinRequestDTO extends MessageDTO {

    private String notificationUid;
    private long notificationCreationTime;
    private String joinRequestUid;
    private String groupUid;
    private String groupName;
    private String joinRequestType;

    @JsonCreator
    public JoinRequestDTO(@JsonProperty("title") String title,
                          @JsonProperty("text") String text,
                          @JsonProperty("notificationUid") String notificationUid,
                          @JsonProperty("notificationCreationTime") long notificationCreationTime,
                          @JsonProperty("groupUid") String groupUid,
                          @JsonProperty("groupName") String groupName,
                          @JsonProperty("joinRequestUid") String joinRequestUid,
                          @JsonProperty("joinRequestType") String joinRequestType
    ) {

        super(title, text);
        this.notificationUid = notificationUid;
        this.notificationCreationTime = notificationCreationTime;
        this.groupName = groupName;
        this.groupUid = groupUid;
        this.joinRequestType = joinRequestType;
        this.joinRequestUid = joinRequestUid;

    }

    public String getNotificationUid() {
        return notificationUid;
    }

    public long getNotificationCreationTime() {
        return notificationCreationTime;
    }

    public String getJoinRequestUid() {
        return joinRequestUid;
    }

    public String getGroupUid() {
        return groupUid;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getJoinRequestType() {
        return joinRequestType;
    }
}
