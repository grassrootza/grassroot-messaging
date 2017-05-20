package za.org.grassroot.messaging.domain;

import za.org.grassroot.messaging.domain.enums.TodoLogType;

import javax.persistence.*;
import java.time.Instant;

/**
 * Created by aakilomar on 12/3/15.
 */
@Entity
@Table(name = "action_todo_log",
		indexes = {@Index(name = "idx_action_todo_log_actiontodo_id", columnList = "action_todo_id")})
public class TodoLog implements TaskLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "uid", nullable = false, unique = true)
	private String uid;

	@Basic
	@Column(name = "created_date_time", nullable = false, updatable = false)
	private Instant createdDateTime;

	@ManyToOne
	@JoinColumn(name = "action_todo_id", nullable = false)
	private Todo todo;

	@Column(name = "message")
	private String message;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name="type", nullable = false, length = 50)
	private TodoLogType type;

	private TodoLog() {
		// for JPA only
	}

	public String getUid() { return uid; }

	public Instant getCreatedDateTime() {
		return createdDateTime;
	}

	public Todo getTodo() {
		return todo;
	}

	public User getUser() {
		return user;
	}

	public String getMessage() {
		return message;
	}

	public TodoLogType getType() {
		return type;
	}

	@Override
	public Task getGroupDescendant() {
		return todo;
	}
}
