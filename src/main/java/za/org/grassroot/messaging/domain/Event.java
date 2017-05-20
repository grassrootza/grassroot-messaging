package za.org.grassroot.messaging.domain;

/*
 * Ported in slim version by luke on 2017/05/19.
 */
import za.org.grassroot.messaging.domain.enums.EventType;
import za.org.grassroot.messaging.domain.enums.TaskType;

import javax.persistence.*;

@Entity
@Table(name = "event")
public class Event implements Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "uid", length = 50, updatable = false)
	protected String uid;

    @Version
	private Integer version;

	@ManyToOne
	@JoinColumn(name = "ancestor_group_id", nullable = false, updatable = false)
	private Group ancestorGroup;

	@Enumerated(EnumType.STRING)
	@Column(name="type", nullable = false, length = 50, updatable = false)
	private EventType eventType;

	protected Event() {
		// for JPA
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public Group getAncestorGroup() {
		return ancestorGroup;
	}

	@Override
	public TaskType getTaskType() {
		return EventType.VOTE.equals(eventType) ?
				TaskType.VOTE : TaskType.MEETING;
	}

}
