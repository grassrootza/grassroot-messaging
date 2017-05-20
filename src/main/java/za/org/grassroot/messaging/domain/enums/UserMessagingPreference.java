package za.org.grassroot.messaging.domain.enums;

/**
 * Created by luke on 2017/05/17.
 * Enum to help distinguish between whether a user prefers our-app notifcations (Android), or SMS, or -- later -- other
 * types of messages as their default. This may also be used for recording messages send and so forth.
 */
public enum UserMessagingPreference {

    SMS,
    ANDROID_APP, // i.e., push notifications within the Grassroot App
    WEB_ONLY // for security/privacy-conscious users who do not want the SMSs and do not have Android

}
