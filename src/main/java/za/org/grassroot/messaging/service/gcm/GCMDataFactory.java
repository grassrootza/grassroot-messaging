package za.org.grassroot.messaging.service.gcm;

import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.group.Group;
import za.org.grassroot.core.domain.task.Task;
import za.org.grassroot.core.domain.task.TaskLog;
import za.org.grassroot.core.enums.NotificationType;
import za.org.grassroot.core.enums.UserLogType;
import za.org.grassroot.messaging.dto.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCMDataFactory {

    public static MessageDTO createGCMDTO(Notification notification) {

        boolean taskRelated = notification.getEventLog() != null || notification.getTodoLog() != null;
        if (taskRelated) {
            Task task = getGroupDescendantLog(notification).getTask();
            Group group = task.getAncestorGroup();
            String title = group.getGroupName();
            String text = notification.getMessage();
            String notificationUid = notification.getUid();
            long creationTime = notification.getCreatedDateTime().toEpochMilli();
            return new EventNotificationDTO(title, text, notificationUid, creationTime, getEventType(task), task.getUid());

        } else if (NotificationType.USER.equals(notification.getNotificationType())) {
            return userNotificationData(notification);
        } else {
            Group group = notification.getRelevantGroup();
            String title = group != null ? group.getGroupName() : "Grassroot";
            String text = notification.getMessage();
            return new DefaultMessageDTO(title, text);
        }

    }


    public static String createCollapseKey(Notification notification) {

        boolean taskRelated = notification.getEventLog() != null || notification.getTodoLog() != null;
        if (taskRelated) {
            return getGroupDescendantLog(notification).getTask().getUid() + "_" +
                    getGroupDescendantLog(notification).getTask().getAncestorGroup().getGroupName();
        } else {
            // means neither event nor to-do log, but may want to handle this differently in future
            return null;
        }
    }


    private static MessageDTO userNotificationData(Notification notification) {

        final UserLogType type = notification.getUserLog().getUserLogType();
        if (isJoinRequestRelated(type)) {

            String groupUid = getGroupUidFromJoinRequestNotification(notification);
            String groupName = getGroupNameFromUserNotification(notification);
            String joinRequestUid = getRequestUidFromJoinRequestNotification(notification);
            String joinRequestType = type.name();
            String title = groupName;
            String text = notification.getMessage();
            String notificationUid = notification.getUid();
            long creationTime = notification.getCreatedDateTime().toEpochMilli();

            return new JoinRequestDTO(title, text, notificationUid, creationTime, groupUid, groupName, joinRequestUid, joinRequestType);

        } else {
            String title = "Grassroot";
            String text = notification.getMessage();
            String notificationUid = notification.getUid();
            String userLogType = type.name();
            long creationTime = notification.getCreatedDateTime().toEpochMilli();
            return new UserLogDTO(title, text, notificationUid, creationTime, userLogType);
        }
    }



    private static TaskLog getGroupDescendantLog(Notification notification) {
        if (NotificationType.EVENT.equals(notification.getNotificationType())) {
            return notification.getEventLog();
        } else if (NotificationType.TODO.equals(notification.getNotificationType())) {
            return notification.getTodoLog();
        } else {
            throw new IllegalArgumentException("Cannot obtain group descendant log from non-task log");
        }
    }


    private static EventType getEventType(Task task) {
        switch (task.getTaskType()) {
            case MEETING:
                return EventType.MEETING;
            case VOTE:
                return EventType.VOTE;
            case TODO:
                return EventType.TO_DO;
        }
        return null;
    }


    private static boolean isJoinRequestRelated(UserLogType userLogType) {
        return UserLogType.JOIN_REQUEST.equals(userLogType) || UserLogType.JOIN_REQUEST_REMINDER.equals(userLogType)
                || UserLogType.JOIN_REQUEST_APPROVED.equals(userLogType) || UserLogType.JOIN_REQUEST_DENIED.equals(userLogType);
    }

    private static String getGroupNameFromUserNotification(Notification notification) {
        final String desc = notification.getUserLog().getDescription();
        final Matcher matchBeg = Pattern.compile("<xgn>").matcher(desc);
        final Matcher matchEnd = Pattern.compile("</xgn>").matcher(desc);
        if (matchBeg.find() && matchEnd.find()) {
            return desc.substring(matchBeg.end(), matchEnd.start());
        } else {
            return "Grassroot";
        }
    }

    private static String getGroupUidFromJoinRequestNotification(Notification notification) {
        final String desc = notification.getUserLog().getDescription();
        final Matcher matchBeg = Pattern.compile("<xguid>").matcher(desc);
        final Matcher matchEnd = Pattern.compile("</xguid>").matcher(desc);
        if (matchBeg.find() && matchEnd.find()) {
            return desc.substring(matchBeg.end(), matchEnd.start());
        } else {
            return null;
        }
    }

    private static String getRequestUidFromJoinRequestNotification(Notification notification) {
        final String desc = notification.getUserLog().getDescription();
        final Matcher matchBeg = Pattern.compile("<xruid>").matcher(desc);
        final Matcher matchEnd = Pattern.compile("</xruid>").matcher(desc);
        if (matchBeg.find() && matchEnd.find()) {
            return desc.substring(matchBeg.end(), matchEnd.start());
        } else {
            return null;
        }
    }


}
