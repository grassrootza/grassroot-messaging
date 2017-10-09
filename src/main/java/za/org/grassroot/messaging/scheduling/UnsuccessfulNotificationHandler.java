package za.org.grassroot.messaging.scheduling;

/**
 * Ported by luke on 2017/05/19.
 * lightweight class to handle resending of unread notifications.
 * note: all sms notifications are automatically marked "read", as that is most secure/reliable message sending mechanism.
 * so this is primarily for handling Android messages that are not read.
 */
public interface UnsuccessfulNotificationHandler {
    void processUnreadNotifications();
}