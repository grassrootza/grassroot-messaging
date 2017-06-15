package za.org.grassroot.messaging.domain;

import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;

import javax.persistence.*;

/**
 * Created by luke on 2017/05/23.
 */
@Embeddable
public class MessageAndRoutingBundle {

    @Column(name = "notification_uid")
    private String notificationUid;

    @Column(name = "phoneNumber")
    private String phoneNumber;

    @Column(name = "message")
    private String message;

    @Column(name = "joined_via_code")
    private boolean joinedViaCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_preference", nullable = false, length = 50)
    private UserMessagingPreference routePreference;

    @Transient
    private String gcmRegistrationId;

    private MessageAndRoutingBundle() {
        // for JPA
    }

    public MessageAndRoutingBundle(String phoneNumber, String message, Boolean userRequested) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.joinedViaCode = userRequested == null ? false : userRequested;
    }

    public MessageAndRoutingBundle(String notificationUid, String phoneNumber, String message,
                                   UserMessagingPreference messagingPreference, boolean joinedViaCode) {
        this.notificationUid = notificationUid;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.joinedViaCode = joinedViaCode;
        this.routePreference = messagingPreference;
    }

    public String getNotificationUid() {
        return notificationUid;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public boolean isJoinedViaCode() {
        return joinedViaCode;
    }

    public UserMessagingPreference getRoutePreference() {
        return routePreference;
    }

    public void setRoutePreference(UserMessagingPreference routePreference) {
        this.routePreference = routePreference;
    }

    public String getGcmRegistrationId() {
        return gcmRegistrationId;
    }

    public void setGcmRegistrationId(String gcmRegistrationId) {
        this.gcmRegistrationId = gcmRegistrationId;
    }

    @Override
    public String toString() {
        return "MessageAndRoutingBundle{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", joinedViaCode=" + joinedViaCode +
                ", message='" + message + '\'' +
                ", routePreference=" + routePreference +
                '}';
    }
}
