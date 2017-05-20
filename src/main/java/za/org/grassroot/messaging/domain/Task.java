package za.org.grassroot.messaging.domain;

import za.org.grassroot.messaging.domain.enums.TaskType;

/**
 * Created by luke on 2017/05/19.
 */
public interface Task {

    String getUid();

    Group getAncestorGroup();

    TaskType getTaskType();

}
