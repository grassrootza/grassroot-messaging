package za.org.grassroot.messaging.service.sms.aws;

import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishResult;
import org.springframework.util.StringUtils;
import za.org.grassroot.messaging.service.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.sms.SmsResponseType;

/**
 * Created by luke on 2017/05/19.
 */
public class AwsSmsResponse implements SmsGatewayResponse {

    private final PublishResult publishResult;
    private final AmazonSNSException error;
    private final boolean successful;
    private final SmsResponseType responseType;


    // looks like AWS SNS is async so don't know if successful right away, but at least can tell was delivered
    public AwsSmsResponse(PublishResult publishResult) {
        this.publishResult = publishResult;
        this.successful = StringUtils.isEmpty(publishResult.getMessageId());
        this.responseType = successful ? SmsResponseType.ROUTED : SmsResponseType.UNKNOWN_ERROR;
        this.error = null;
    }

    public AwsSmsResponse(AmazonSNSException e) {
        this.successful = false;
        this.error = e;
        this.responseType = SmsResponseType.UNKNOWN_ERROR; // todo: extract from exception
        this.publishResult = null;
    }

    @Override
    public SmsResponseType getResponseType() {
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
    public String getMessageKey() {
        return publishResult.getMessageId();
    }

    @Override
    public String toString() {
        return "AwsSmsResponse{" +
                "publishResult=" + publishResult +
                '}';
    }
}
