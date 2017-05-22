package za.org.grassroot.messaging.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "group_profile") // quoting table name in case "group" is a reserved keyword
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true, updatable = false)
    private String uid;

    @Column(name = "name", nullable = false, length = 50, updatable = false)
    private String groupName;

    @Column(name = "created_date_time", insertable = true, updatable = false)
    private Instant createdDateTime;

    @ManyToOne()
    @JoinColumn(name = "created_by_user", nullable = false, updatable = false)
    private User createdByUser;

    private Group() {
        // for JPA
    }

    public String getUid() {
        return uid;
    }

    public String getGroupName() {
        return groupName;
    }

    public Instant getCreatedDateTime() {
        return createdDateTime;
    }

    public User getCreatedByUser() {
        return this.createdByUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Group)) {
            return false;
        }

        Group group = (Group) o;

        return (getUid() != null) ? getUid().equals(group.getUid()) : group.getUid() == null;

    }



}
