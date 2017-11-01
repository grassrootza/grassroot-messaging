package za.org.grassroot.messaging.service.gcm;

import za.org.grassroot.core.domain.UserLog;
import za.org.grassroot.core.domain.task.EventLog;
import za.org.grassroot.core.domain.task.TodoLog;

/**
 * Created by luke on 2016/07/27.
 */
public enum AndroidClickActionType {

	VIEW_TASK,
	TASK_CREATED,
	TASK_REMINDER,
	TASK_CHANGED,
	TASK_CANCELLED,
	SHOW_JOIN_REQ,
	JOIN_APPROVED,
	TASK_RESULTS,
	SHOW_MESSAGE,
	CHAT_MESSAGE;

	public static AndroidClickActionType fromEventLog(EventLog eventLog) {
		switch (eventLog.getEventLogType()) {
			case CREATED:
				return TASK_CREATED;
			case CHANGE:
				return TASK_CHANGED;
			case CANCELLED:
				return TASK_CANCELLED;
			case REMINDER:
				return TASK_REMINDER;
			case RESULT:
				return TASK_RESULTS;
			default:
				return SHOW_MESSAGE;
		}
	}

	public static AndroidClickActionType fromTodoLog(TodoLog todoLog) {
		switch (todoLog.getType()) {
			case CREATED:
				return TASK_CREATED;
			case CHANGED:
				return TASK_CHANGED;
			case REMINDER_SENT:
				return TASK_REMINDER;
			default:
				return SHOW_MESSAGE;
		}
	}

	public static AndroidClickActionType fromUserLog(UserLog userLog) {
		switch (userLog.getUserLogType()) {
			case JOIN_REQUEST:
			case JOIN_REQUEST_REMINDER:
				return AndroidClickActionType.SHOW_JOIN_REQ;
			case JOIN_REQUEST_APPROVED:
				return AndroidClickActionType.JOIN_APPROVED;
			default:
				return AndroidClickActionType.SHOW_MESSAGE;
		}
	}

}
