package za.org.grassroot.messaging.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import za.org.grassroot.messaging.domain.enums.NotificationDetailedType;
import za.org.grassroot.messaging.domain.enums.NotificationType;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;

import javax.persistence.*;
import java.time.Instant;

/**
 * Created by luke on 2017/05/17.
 */
@Entity
@Table(name = "notification")
@DynamicUpdate
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid;

    @Column(name = "creation_time", insertable = true, updatable = false)
    private Instant createdDateTime;


    @Column(name = "attempt_count", nullable = false)
    private int sendAttempts = 0;

    @ManyToOne
    @JoinColumn(name = "target_id")
    private User target;


    @Column(name = "message")
    protected String message;


    @Column(name = "sending_status")
    private NotificationStatus status = NotificationStatus.READY_TO_SEND;

    @Column(name = "send_only_after")
    private Instant sendOnlyAfter;

    @Column(name = "last_status_change")
    private Instant lastStatusChange;


    @Setter
    @Column(name = "sending_key")
    protected String sendingKey;

    @Setter
    @Column(name = "delivery_channel")
    public UserMessagingPreference deliveryChannel;


    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable = false, length = 50)
    private NotificationDetailedType detailedType;

    @ManyToOne
    @JoinColumn(name = "event_log_id")
    private EventLog eventLog;

    @ManyToOne
    @JoinColumn(name = "action_todo_log_id")
    private TodoLog todoLog;

    @ManyToOne
    @JoinColumn(name = "user_log_id", foreignKey = @ForeignKey(name = "fk_notification_user_log"))
    private UserLog userLog;

    @ManyToOne
    @JoinColumn(name = "group_log_id", foreignKey = @ForeignKey(name = "fk_notification_group_log"))
    private GroupLog groupLog;


    // for testing
    public static Notification makeDummy(String message) {
        Notification notification = new Notification();
        notification.message = message;
        return notification;
    }

    private Notification() {
        // for JPA
    }

    public void updateStatus(NotificationStatus status) {
        this.status = status;
        this.lastStatusChange = Instant.now();
        if (status == NotificationStatus.SENT || status == NotificationStatus.SENDING_FAILED)
            this.sendAttempts++;
    }


    public NotificationType getType() { return NotificationType.fromDetailedType(detailedType); }


    public TaskLog getGroupDescendantLog() {
        if (NotificationType.EVENT.equals(getType())) {
            return eventLog;
        } else if (NotificationType.TODO.equals(getType())) {
            return todoLog;
        } else {
            throw new IllegalArgumentException("Cannot obtain group descendant log from non-task log");
        }
    }

    public boolean hasGroupLog() { return groupLog != null; }

    public boolean isTaskRelated() {
        return eventLog != null || todoLog != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notification that = (Notification) o;

        return uid != null ? uid.equals(that.uid) : that.uid == null;
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }


    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", target=" + target.getPhoneNumber() +
                ", message='" + message + '\'' +
                '}';
    }
}