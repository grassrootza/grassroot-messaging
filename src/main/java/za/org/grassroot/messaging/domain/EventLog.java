package za.org.grassroot.messaging.domain;

/*
 * Ported in slim version by luke on 2017/05/19.
 */
import za.org.grassroot.messaging.domain.enums.EventLogType;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name="event_log")
public class EventLog implements TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true, updatable = false)
    private String uid;

    @Column(name="created_date_time", nullable = false, updatable = false)
    private Instant createdDateTime;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name="event_log_type", nullable = false, length = 50)
    private EventLogType eventLogType;

    /*
    Constructors
     */

    private EventLog() {
        // for JPA
    }

    public String getUid() { return uid; }

    public Instant getCreatedDateTime() {
        return createdDateTime;
    }

    public Event getEvent() {
        return event;
    }

    public EventLogType getEventLogType() {
        return eventLogType;
    }

    @Override
    public Task getGroupDescendant() {
        return event;
    }
}
