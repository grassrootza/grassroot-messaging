package za.org.grassroot.messaging.service.gcm;

import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.GcmRegistration;
import za.org.grassroot.core.domain.Group;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.domain.task.EventLog;
import za.org.grassroot.core.domain.task.Task;
import za.org.grassroot.core.domain.task.TaskLog;
import za.org.grassroot.core.domain.task.TodoLog;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.enums.NotificationType;
import za.org.grassroot.core.enums.UserLogType;
import za.org.grassroot.core.repository.GcmRegistrationRepository;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.util.DebugUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by luke on 2017/05/19.
 */
@Service
@ConditionalOnProperty(value = "grassroot.gcm.enabled", havingValue = "true")
public class PushNotificationBrokerImpl implements PushNotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationBrokerImpl.class);

    private final GcmHandlingBroker sendingService;

    private final GcmRegistrationRepository gcmRegistrationRepository;

    private final NotificationBroker notificationBroker;

    @Autowired
    public PushNotificationBrokerImpl(GcmHandlingBroker sendingService,
                                      GcmRegistrationRepository gcmRegistrationRepository, NotificationBroker notificationBroker) {
        this.sendingService = sendingService;
        this.gcmRegistrationRepository = gcmRegistrationRepository;
        this.notificationBroker = notificationBroker;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendMessage(Message message) {
        logger.info("sending message via GCM sender ...");
        Notification notification = (Notification) message.getPayload();
        sendingService.sendGcmMessage(buildGcmFromMessagePayload(notification));
        notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENT, null, true,
                notification.getUid(), MessagingProvider.GCM);

    }

    private GcmPayload buildGcmFromMessagePayload(Notification notification) {
        DebugUtil.transactionRequired(GcmXmppBrokerImpl.class);
        GcmRegistration registration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(notification.getTarget());
        return new GcmPayload(notification.getUid(),
                registration.getRegistrationId(),
                generateCollapseKey(notification),
                generateData(notification), null);
    }

    private String generateCollapseKey(Notification notification) {
        boolean taskRelated = notification.getEventLog() != null || notification.getTodoLog() != null;
        if (taskRelated) {
            return getGroupDescendantLog(notification).getTask().getUid() + "_" +
                    getGroupDescendantLog(notification).getTask().getAncestorGroup().getGroupName();
        } else {
            // means neither event nor to-do log, but may want to handle this differently in future
            return null;
        }
    }

    private TaskLog getGroupDescendantLog(Notification notification) {
        if (NotificationType.EVENT.equals(notification.getNotificationType())) {
            return notification.getEventLog();
        } else if (NotificationType.TODO.equals(notification.getNotificationType())) {
            return notification.getTodoLog();
        } else {
            throw new IllegalArgumentException("Cannot obtain group descendant log from non-task log");
        }
    }

    private Map<String, Object> generateData(Notification notification) {
        Map<String, Object> data = new HashMap<>();
        boolean taskRelated = notification.getEventLog() != null || notification.getTodoLog() != null;
        if (taskRelated) {
            Task task = getGroupDescendantLog(notification).getTask();
            Group group = task.getAncestorGroup();
            data = assembleMap(group.getGroupName(),
                    group.getGroupName(),
                    group.getUid(),
                    task.getUid(),
                    task.getTaskType().name(),
                    notification);
        } else if (NotificationType.USER.equals(notification.getNotificationType())) {
            return userNotificationData(notification);
        }
        return data;
    }

    private Map<String, Object> assembleMap(final String title, final String group, String groupUid, final String entityUid,
                                            final String entityType, Notification notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        if (group != null) {
            data.put("group", group);
        }
        if (!TextUtils.isEmpty(groupUid)) {
            data.put("group_uid", groupUid);
        }
        data.put("notification_uid", notification.getUid());
        data.put("body", notification.getMessage());
        data.put("entity_uid", entityUid);
        data.put("created_date_time", notification.getCreatedDateTime().toEpochMilli());
        data.put("alert_type", notification.getNotificationType());
        data.put("entity_type", entityType);
        data.put("click_action", getActionType(notification));

        return data;
    }

    private AndroidClickActionType getActionType(Notification notification) {
        AndroidClickActionType actionType;
        switch (notification.getNotificationType()) {
            case EVENT:
                EventLog eventLog = notification.getEventLog();
                actionType = AndroidClickActionType.fromEventLog(eventLog);
                break;
            case TODO:
                TodoLog todoLog = notification.getTodoLog();
                actionType = AndroidClickActionType.fromTodoLog(todoLog);
                break;
            case USER:
                actionType = AndroidClickActionType.fromUserLog(notification.getUserLog());
                break;
            default:
                actionType = AndroidClickActionType.SHOW_MESSAGE;
        }
        return actionType;
    }

    private Map<String, Object> userNotificationData(Notification notification) {
        final UserLogType type = notification.getUserLog().getUserLogType();
        if (isJoinRequestRelated(type)) {
            return assembleMap(
                    getGroupNameFromUserNotification(notification),
                    getGroupUidFromJoinRequestNotification(notification),
                    getGroupUidFromJoinRequestNotification(notification),
                    getRequestUidFromJoinRequestNotification(notification),
                    type.name(),
                    notification);
        } else {
            return assembleMap(
                    "Grassroot", null, null, null,
                    type.name(),
                    notification);
        }
    }

    private boolean isJoinRequestRelated(UserLogType userLogType) {
        return UserLogType.JOIN_REQUEST.equals(userLogType) || UserLogType.JOIN_REQUEST_REMINDER.equals(userLogType)
                || UserLogType.JOIN_REQUEST_APPROVED.equals(userLogType) || UserLogType.JOIN_REQUEST_DENIED.equals(userLogType);
    }

    private String getGroupNameFromUserNotification(Notification notification) {
        final String desc = notification.getUserLog().getDescription();
        final Matcher matchBeg = Pattern.compile("<xgn>").matcher(desc);
        final Matcher matchEnd = Pattern.compile("</xgn>").matcher(desc);
        if (matchBeg.find() && matchEnd.find()) {
            return desc.substring(matchBeg.end(), matchEnd.start());
        } else {
            return "Grassroot";
        }
    }

    private String getGroupUidFromJoinRequestNotification(Notification notification) {
        final String desc = notification.getUserLog().getDescription();
        final Matcher matchBeg = Pattern.compile("<xguid>").matcher(desc);
        final Matcher matchEnd = Pattern.compile("</xguid>").matcher(desc);
        if (matchBeg.find() && matchEnd.find()) {
            return desc.substring(matchBeg.end(), matchEnd.start());
        } else {
            return null;
        }
    }

    private String getRequestUidFromJoinRequestNotification(Notification notification) {
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
