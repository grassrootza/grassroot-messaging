package za.org.grassroot.messaging.domain;


import za.org.grassroot.core.domain.Group;
import za.org.grassroot.core.domain.User;

import javax.persistence.*;
import java.time.Instant;

/**
 * Created by paballo on 2016/11/06.
 */
@Entity
@Table(name = "group_message_stats")
public class GroupChatMessageStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true)
    private String messageUid;

    @Column(name = "created_date_time", insertable = true, updatable = false)
    private Instant createdDateTime;

    @ManyToOne()
    @JoinColumn(name = "intended_group", nullable = false, updatable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "sender", nullable = false, updatable = false)
    private User user;

    @Column(name="number_intended_recepients")
    private Long intendedReceipients;

    @Column(name = "number_times_read")
    private Long timesRead;

    @Column(name="read")
    private Boolean read;

    private GroupChatMessageStats(){
        // for JPA
    }

    public GroupChatMessageStats(String messageUid, Group group, User user, Long intendedReceipients, Long timesRead, Boolean read){
        this.messageUid = messageUid;
        this.group = group;
        this.user =user;
        this.intendedReceipients = intendedReceipients;
        this.timesRead=timesRead;
        this.read=read;
        this.createdDateTime=Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getMessageUid() {
        return messageUid;
    }

    public Instant getCreatedDateTime() {
        return createdDateTime;
    }

    public Group getGroup() {
        return group;
    }

    public Long getIntendedReceipients() {
        return intendedReceipients;
    }

    public Long getTimesRead() {
        return timesRead;
    }

    public Boolean isRead() {
        return read;
    }

    public User getUser() {
        return user;
    }

    public void incrementReadCount(){
        this.timesRead = ++timesRead;

    }

    public void setRead(Boolean read) {
        this.read = read;
    }
}
