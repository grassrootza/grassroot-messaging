package za.org.grassroot.messaging.domain;

import org.hibernate.annotations.DynamicUpdate;
import za.org.grassroot.messaging.domain.enums.UserMessagingPreference;

import javax.persistence.*;

/**
 * Created by luke on 2017/05/17.
 */
@Entity
@Table(name = "user_profile")
@DynamicUpdate
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true, updatable = false)
    private String uid;

    @Column(name = "phone_number", nullable = false, length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name", nullable = true, length = 70) // allowing this to be nullable as might not be set
    private String displayName;

    @Column(name = "language_code", nullable = true, length = 10)
    private String languageCode;

    @Column(name = "android")
    private boolean hasAndroidProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_preference", nullable = false, length = 50)
    private UserMessagingPreference messagingPreference;

    @Version
    private Integer version;

    private User() {
        // for JPA
    }

    public String getUid() { return uid; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public UserMessagingPreference getMessagingPreference() {
        return messagingPreference;
    }

    public void setMessagingPreference(UserMessagingPreference messagingPreference) {
        this.messagingPreference = messagingPreference;
    }
}
