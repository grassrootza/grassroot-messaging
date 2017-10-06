package za.org.grassroot.messaging.service.sms.aws;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.org.grassroot.messaging.service.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.sms.SmsSendingService;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luke on 2017/05/18.
 */
@Service("awsSmsSender")
@ConditionalOnProperty(name = "grassroot.aws.sms.enabled", havingValue = "true", matchIfMissing = false)
public class AwsSmsSendingManager implements SmsSendingService {

    private static final Logger logger = LoggerFactory.getLogger(AwsSmsSendingManager.class);

    @Value("${grassroot.aws.test.topic:}")
    private String awsTestTopicArn; // for testing on AWS SNS

    @Value("${grassroot.aws.test.subscribe:false}")
    private boolean awsTestTopicSubscribe;

    private final Environment environment;
    private final AmazonSNS snsClient;

    @Autowired
    public AwsSmsSendingManager(Environment environment) {
        this.environment = environment;
        this.snsClient = AmazonSNSClientBuilder.standard()
                .withRegion(Regions.EU_WEST_1)
                .withCredentials(new ProfileCredentialsProvider("snsSender"))
                .build();
    }

    @PostConstruct
    public void init() {
        if (awsTestTopicSubscribe && !StringUtils.isEmpty(awsTestTopicArn) && !environment.acceptsProfiles("production")) {
            snsClient.subscribe(new SubscribeRequest()
                    .withTopicArn(awsTestTopicArn)
                    .withProtocol("email")
                    .withEndpoint("contact@grassroot.org.za"));
            logger.info("AWS started up and subscribed to test topic: {}", awsTestTopicArn);
        } else {
            logger.info("AWS sending manager started up ...");
        }
    }

    @Override
    public SmsGatewayResponse sendSMS(String message, String destinationNumber) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("Sending AWS SMS to {}, ...", destinationNumber);
            PublishResult result = snsClient.publish(smsPublishRequest(message, destinationNumber, "Promotional"));
            logger.debug("SMS sent via AWS, result in ms: {}", System.currentTimeMillis() - startTime);
            return new AwsSmsResponse(result);
        } catch (AmazonSNSException e) {
            logger.error(e.getMessage());
            return new AwsSmsResponse(e);
        }
    }

    @Override
    public SmsGatewayResponse sendPrioritySMS(String message, String destinationNumber) {
        return null;
    }

    private PublishRequest smsPublishRequest(String message, String destinationNumber, String messageType) {
        if (environment.acceptsProfiles("production")) {
            return new PublishRequest()
                    .withMessage(message)
                    .withPhoneNumber(destinationNumber)
                    .withMessageAttributes(propertiesMap(messageType));
        } else {
            return new PublishRequest()
                    .withMessage(message)
                    .withTopicArn(awsTestTopicArn)
                    .withMessageAttributes(propertiesMap(messageType));
        }
    }

    private Map<String, MessageAttributeValue> propertiesMap(String smsType) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
        smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                .withStringValue("Grassroot") //The sender ID shown on the device.
                .withDataType("String"));
        smsAttributes.put("AWS.SNS.SMS.MaxPrice", new MessageAttributeValue()
                .withStringValue("0.06") //Sets the max price to 0.05 USD.
                .withDataType("Number"));
        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue(smsType) //Sets the type to promotional.
                .withDataType("String"));
        return smsAttributes;
    }

}
