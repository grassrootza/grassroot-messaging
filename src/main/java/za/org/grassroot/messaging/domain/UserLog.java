package za.org.grassroot.messaging.domain;

import za.org.grassroot.messaging.domain.enums.UserLogType;

import javax.persistence.*;
import java.time.Instant;

import static za.org.grassroot.messaging.domain.enums.UserLogType.*;

/**
 * Created by luke on 2016/02/22.
 */
@Entity
@Table(name="user_log",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_log_request_uid", columnNames = "uid")})
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, length = 50)
    private String uid;

    @Column(name="creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    @Enumerated(EnumType.STRING)
    @Column(name="user_log_type", nullable = false, length = 50)
    private UserLogType userLogType;

    @ManyToOne
    @JoinColumn(name="user_uid", referencedColumnName = "uid", nullable = false)
    private User user;

    @Column(name="description", length = 255)
    private String description;

    private UserLog() {
        // for JPA
    }

    public String getUid() { return uid; }

    public UserLogType getUserLogType() {
        return userLogType;
    }

    public User getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }

    public boolean isJoinRequestRelated() {
        return JOIN_REQUEST.equals(userLogType) || JOIN_REQUEST_REMINDER.equals(userLogType)
                || JOIN_REQUEST_APPROVED.equals(userLogType) || JOIN_REQUEST_DENIED.equals(userLogType);
    }

    @Override
    public int hashCode() {
        return (getUid() != null) ? getUid().hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        final UserLog that = (UserLog) o;

        return getUid() != null ? getUid().equals(that.getUid()) : that.getUid() == null;

    }

    @Override
    public String toString() {
        return "UserLog{" +
                "id=" + id +
                ", userLogType=" + userLogType +
                ", userUid=" + user.getUid() +
                ", description='" + description + '\'' +
                ", creationTime =" + creationTime +
                '}';
    }
}
