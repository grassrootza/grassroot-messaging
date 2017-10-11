package za.org.grassroot.messaging.service.sms;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.PriorityMessage;
import za.org.grassroot.messaging.service.NotificationBroker;

import javax.annotation.PostConstruct;

/**
 * Created by luke on 2017/05/18.
 */
@Service
public class SmsNotificationBrokerImpl implements SmsNotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationBrokerImpl.class);

    @Value("${grassroot.sms.sending.awsdefault:false}")
    private boolean routeAllThroughAws;

    private final NotificationBroker notificationBroker;

    private SmsSendingService defaultSmsSender;
    private SmsSendingService aatSmsSender;
    private SmsSendingService awsSmsSender;

    // we require at least one bean wired up
    @Autowired
    public SmsNotificationBrokerImpl(NotificationBroker notificationBroker, SmsSendingService smsSendingService) {
        this.notificationBroker = notificationBroker;
        this.defaultSmsSender = smsSendingService;
    }

    @Autowired(required = false)
    public void setAatSmsSender(@Qualifier("aatSmsSender") SmsSendingService aatSmsSender) {
        this.aatSmsSender = aatSmsSender;
    }

    @Autowired(required = false)
    public void setS3SmsSender(@Qualifier("awsSmsSender") SmsSendingService awsSmsSender) {
        this.awsSmsSender = awsSmsSender;
    }

    @PostConstruct
    public void init() {
        if (awsSmsSender != null && aatSmsSender != null) {
            defaultSmsSender = routeAllThroughAws ? awsSmsSender : aatSmsSender;
        }
    }

    @Timed
    @Override
    public void sendStandardSmsNotification(Message message) {
        logger.debug("Handling SMS message, no strategy specified, sending by default ...");
        MessageAndRoutingBundle messagePayload = (MessageAndRoutingBundle) message.getPayload();
        SmsGatewayResponse response = awsSmsSender != null && messagePayload.isJoinedViaCode() ?
                awsSmsSender.sendSMS(messagePayload.getMessage(), messagePayload.getPhoneNumber()) :
                defaultSmsSender.sendSMS(messagePayload.getMessage(), messagePayload.getPhoneNumber());

        handleSmsGatewayResponse(messagePayload.getNotificationUid(), response);
    }

    @Timed
    @Override
    public void sendSmsNotificationByStrategy(Message message, SmsSendingStrategy strategy) {
        logger.debug("Sending with strategy: {}", strategy.name());
        Notification notification = (Notification) message.getPayload();
        SmsGatewayResponse response;
        switch (strategy) {
            case DEFAULT:
                sendStandardSmsNotification(message);
                response = null; // since sendStandard will take care of handling response
                break;
            case AAT:
                if (aatSmsSender == null) {
                    throw new IllegalArgumentException("Error! AAT sending requested without bean wired");
                }
                response = aatSmsSender.sendSMS(notification.getMessage(), notification.getTarget().getPhoneNumber());
                break;
            case AWS:
                if (awsSmsSender == null) {
                    throw new IllegalArgumentException("Error! AWS sending requested without bean wired");
                }
                response = awsSmsSender.sendSMS(notification.getMessage(), notification.getTarget().getPhoneNumber());
                break;
            default:
                throw new IllegalArgumentException("Unsupported SMS strategy provided");
        }

        if (response != null) {
            handleSmsGatewayResponse(notification.getUid(), response);
        }
    }

    @Override
    public void sendSmsWithoutNotification(Message message) {
        MessageAndRoutingBundle payload = (MessageAndRoutingBundle) message.getPayload();
        if (awsSmsSender != null && payload.isJoinedViaCode()) {
            awsSmsSender.sendSMS(payload.getMessage(), payload.getPhoneNumber());
        }  else {
            defaultSmsSender.sendSMS(payload.getMessage(), payload.getPhoneNumber());
        }
    }


    @Override
    public void sendPrioritySmsNotification(Message message) {
        PriorityMessage payload = (PriorityMessage) message.getPayload();
        logger.debug("Inside broker, handling priority message: {}", payload);
        defaultSmsSender.sendPrioritySMS(payload.getMessage(), payload.getPhoneNumber());
    }

    @Override
    public void sendSmsNotificationOnError(Message message) {
        logger.info("Should really wire up error handling");
    }


    private void handleSmsGatewayResponse(String notificationUid, SmsGatewayResponse response) {

        if (response != null && response.isSuccessful()) {
            notificationBroker.updateNotificationStatus(notificationUid, NotificationStatus.SENT, null, response.getMessageKey());
        } else if (response != null && response.getResponseType() != null) {

            switch (response.getResponseType()) {
                case MSISDN_INVALID:
                    logger.info("invalid number for SMS, marking it as read to prevent looping redelivery");
                    notificationBroker.updateNotificationStatus(notificationUid, NotificationStatus.UNDELIVERABLE, "Can't send message. Invalid MSISDN.", null);
                    break;
                case DUPLICATE_MESSAGE:
                    logger.info("trying to resend message, just set it as read");
                    notificationBroker.updateNotificationStatus(notificationUid, NotificationStatus.DELIVERED, "Can't send message. Duplicate message", null); //todo(beegor), hmm, what to do here ?
                    break;
                default:
                    notificationBroker.updateNotificationStatus(notificationUid, NotificationStatus.SENDING_FAILED, "Can't send message. Reason: " + response.getResponseType(), null);
                    logger.error("error delivering SMS, response from gateway: {}", response.toString());
            }
        } else {
            logger.error("Error delivering SMS, with null response");
        }
        logger.debug("finished updating read and delivered status");
    }
}