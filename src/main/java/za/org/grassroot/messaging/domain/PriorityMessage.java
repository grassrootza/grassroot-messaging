package za.org.grassroot.messaging.domain;

/**
 * Created by luke on 2017/05/20.
 */
public class PriorityMessage {

    private String phoneNumber;
    private String message;

    public PriorityMessage(String phoneNumber, String message) {
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "PriorityMessage{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
