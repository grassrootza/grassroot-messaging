package za.org.grassroot.messaging.domain;

import org.hibernate.annotations.DynamicUpdate;
import za.org.grassroot.messaging.domain.enums.NotificationDetailedType;
import za.org.grassroot.messaging.domain.enums.NotificationType;

import javax.persistence.*;
import java.time.Instant;

/**
 * Created by luke on 2017/05/17.
 */
@Entity
@Table(name = "notification")
@DynamicUpdate
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid;

    @Column(name = "creation_time", insertable = true, updatable = false)
    private Instant createdDateTime;

    @Column(name = "next_attempt_time")
    private Instant nextAttemptTime;

    @Column(name = "last_attempt_time")
    private Instant lastAttemptTime;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @ManyToOne
    @JoinColumn(name = "target_id")
    private User target;

    @Column(name = "read")
    private boolean read = false;

    @Column(name = "delivered")
    private boolean delivered = false;

    @Column(name = "for_android_tl")
    private boolean forAndroidTimeline = false;

    @Column(name = "viewed_android")
    private boolean viewedOnAndroid = false;

    @Column(name = "message")
    protected String message;

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

    // for testing
    public static Notification makeDummy(String message) {
        Notification notification = new Notification();
        notification.message = message;
        return notification;
    }

    private Notification() {
        // for JPA
    }

    public void markAsDelivered() {
        this.delivered = true;
        this.nextAttemptTime = null;
    }

    public void markReadAndViewed() {
        this.delivered = true;
        this.read = true;
        this.viewedOnAndroid = true;
    }

    public String getUid() {
        return uid;
    }

    public Instant getCreatedDateTime() {
        return createdDateTime;
    }

    public Instant getNextAttemptTime() {
        return nextAttemptTime;
    }

    public void setNextAttemptTime(Instant nextAttemptTime) {
        this.nextAttemptTime = nextAttemptTime;
    }

    public Instant getLastAttemptTime() {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(Instant lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    public User getTarget() {
        return target;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) { this.read = read; }

    public boolean isDelivered() {
        return delivered;
    }

    public boolean isForAndroidTimeline() {
        return forAndroidTimeline;
    }

    public boolean isViewedOnAndroid() {
        return viewedOnAndroid;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() { return NotificationType.fromDetailedType(detailedType); }

    public EventLog getEventLog() {
        return eventLog;
    }

    public TodoLog getTodoLog() {
        return todoLog;
    }

    public UserLog getUserLog() {
        return userLog;
    }

    public TaskLog getGroupDescendantLog() {
        if (NotificationType.EVENT.equals(getType())) {
            return eventLog;
        } else if (NotificationType.TODO.equals(getType())) {
            return todoLog;
        } else {
            throw new IllegalArgumentException("Cannot obtain group descendant log from non-task log");
        }
    }

    public boolean isTaskRelated() {
        return NotificationType.EVENT.equals(getType()) || NotificationType.TODO.equals(getType());
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
}
