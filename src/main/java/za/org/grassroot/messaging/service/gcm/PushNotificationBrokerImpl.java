package za.org.grassroot.messaging.service.gcm;

import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.messaging.domain.*;
import za.org.grassroot.messaging.domain.enums.NotificationType;
import za.org.grassroot.messaging.domain.enums.UserLogType;
import za.org.grassroot.messaging.util.DebugUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by luke on 2017/05/19.
 */
@Service
public class PushNotificationBrokerImpl implements PushNotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationBrokerImpl.class);

    private final GcmHandlingBroker sendingService;
    private final GcmRegistrationRepository gcmRegistrationRepository;

    @Autowired
    public PushNotificationBrokerImpl(GcmHandlingBroker sendingService, GcmRegistrationRepository gcmRegistrationRepository) {
        this.sendingService = sendingService;
        this.gcmRegistrationRepository = gcmRegistrationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendMessage(Message message) {
        logger.info("sending message via GCM sender ...");
        sendingService.sendGcmMessage(buildGcmFromNotification((Notification) message.getPayload()));
    }

    private GcmPayload buildGcmFromNotification(Notification notification) {
        DebugUtil.transactionRequired(GcmXmppBrokerImpl.class);
        GcmRegistration gcmRegistration = gcmRegistrationRepository.findTopByUserOrderByCreationTimeDesc(notification.getTarget());
        return new GcmPayload(notification.getUid(),
                gcmRegistration.getUid(),
                generateCollapseKey(notification),
                generateData(notification), null);
    }

    private String generateCollapseKey(Notification notification) {
        if (notification.isTaskRelated()) {
            return notification.getGroupDescendantLog().getGroupDescendant().getUid() + "_" +
                    notification.getGroupDescendantLog().getGroupDescendant().getAncestorGroup().getGroupName();
        } else {
            // means neither event nor to-do log, but may want to handle this differently in future
            return null;
        }
    }

    private Map<String, Object> generateData(Notification notification) {
        Map<String, Object> data = new HashMap<>();
        if (notification.isTaskRelated()) {
            Task task = notification.getGroupDescendantLog().getGroupDescendant();
            Group group = task.getAncestorGroup();
            data = assembleMap(group.getGroupName(),
                    group.getGroupName(),
                    group.getUid(),
                    task.getUid(),
                    task.getTaskType().name(),
                    notification);
        } else if (NotificationType.USER.equals(notification.getType())) {
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
            data.put("groupUid", groupUid);
        }
        data.put("notificationUid", notification.getUid());
        data.put("body", notification.getMessage());
        data.put("id", entityUid);
        data.put("created_date_time", notification.getCreatedDateTime());
        data.put("alert_type", notification.getType());
        data.put("entity_type", entityType);
        data.put("click_action", getActionType(notification));

        return data;
    }

    private AndroidClickActionType getActionType(Notification notification) {
        AndroidClickActionType actionType;
        switch (notification.getType()) {
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
        if (notification.getUserLog().isJoinRequestRelated()) {
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
