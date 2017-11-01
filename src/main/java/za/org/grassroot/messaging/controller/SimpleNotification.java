package za.org.grassroot.messaging.controller;

import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.domain.UserLog;
import za.org.grassroot.core.enums.NotificationDetailedType;
import za.org.grassroot.core.enums.NotificationType;
import za.org.grassroot.core.enums.UserInterfaceType;
import za.org.grassroot.core.enums.UserLogType;

public class SimpleNotification extends Notification {

    public SimpleNotification(User target, String message, Boolean useOnlyFreeChannels) {
        super(target, message, new UserLog(target.getUid(), UserLogType.USER_SESSION, "Dummy user log, since notification requires it", UserInterfaceType.SYSTEM));
        setUseOnlyFreeChannels(useOnlyFreeChannels);
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.GENERAL;
    }

    @Override
    public NotificationDetailedType getNotificationDetailedType() {
        return null;
    }

    @Override
    protected void appendToString(StringBuilder stringBuilder) {
    }
}