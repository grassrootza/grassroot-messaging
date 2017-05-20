package za.org.grassroot.messaging.domain;

import za.org.grassroot.messaging.domain.enums.TaskType;

import javax.persistence.*;

/**
 * Created by aakilomar on 12/3/15.
 */
@Entity
@Table(name = "action_todo",
        indexes = {@Index(name = "idx_action_todo_ancestor_group_id", columnList = "ancestor_group_id")})
public class Todo implements Task {

    // private static final Logger logger = LoggerFactory.getLogger();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    protected Long id;

    @Column(name = "uid", nullable = false, unique = true)
    protected String uid;

    @ManyToOne
   	@JoinColumn(name = "ancestor_group_id", nullable = false)
   	private Group ancestorGroup;

    private Todo() {
        // for JPA
    }

    public String getUid() {
        return uid;
    }

    @Override
    public Group getAncestorGroup() {
        return ancestorGroup;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TODO;
    }
}
