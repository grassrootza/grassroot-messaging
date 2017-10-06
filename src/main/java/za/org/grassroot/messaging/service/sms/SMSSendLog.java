package za.org.grassroot.messaging.service.sms;

import java.util.ArrayList;
import java.util.List;

public class SMSSendLog {


    private List<SentSMSStatus> sentMessagesStatuses = new ArrayList<>();

    public SMSSendLog(List<SentSMSStatus> sentMessagesStatuses) {

        this.sentMessagesStatuses = sentMessagesStatuses;
    }

    public List<SentSMSStatus> getSentMessagesStatuses() {
        return sentMessagesStatuses;
    }
}
