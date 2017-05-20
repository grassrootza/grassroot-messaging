package za.org.grassroot.messaging.domain.sms;

import com.amazonaws.services.sns.model.PublishResult;

/**
 * Created by luke on 2017/05/19.
 */
public class AwsSmsResponse implements SmsGatewayResponse {

    private final PublishResult publishResult;
    private final boolean successful;

    // looks like AWS SNS is async so don't know if successful right away
    public AwsSmsResponse(PublishResult publishResult) {
        this.publishResult = publishResult;
        this.successful = true;
    }

    @Override
    public SmsResponseType getResponseType() {
        // AWS does not tell us immediately, hence ..
        return SmsResponseType.ROUTED;
    }

    @Override
    public boolean isSuccessful() {
        return true;
    }

    @Override
    public Integer getErrorCode() {
        return null;
    }

    @Override
    public String toString() {
        return "AwsSmsResponse{" +
                "publishResult=" + publishResult +
                '}';
    }
}
