package za.org.grassroot.messaging.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import za.org.grassroot.messaging.domain.Notification;
import za.org.grassroot.messaging.domain.sms.SmsGatewayResponse;
import za.org.grassroot.messaging.service.NotificationBroker;

/**
 * Created by luke on 2017/05/18.
 */
@Service
public class SmsNotificationBrokerImpl implements SmsNotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationBrokerImpl.class);

    @Value("${grassroot.sms.sending.awsdefault:false}")
    private boolean routeAllThroughAws;

    private final NotificationBroker notificationBroker;
    private final SmsSendingService defaultSmsSender;

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

    @Override
    public void sendStandardSmsNotification(Message message) {
        logger.info("Handling SMS message, no strategy specified, sending by default ...");
        Notification notification = (Notification) message.getPayload();
        SmsGatewayResponse response = defaultSmsSender
                .sendSMS(notification.getMessage(), notification.getTarget().getPhoneNumber());
        updateReadAndDeliveredStatus(notification, response);
    }

    @Override
    public void sendSmsNotificationByStrategy(Message message, SmsSendingStrategy strategy) {
        logger.info("Sending with strategy: {}", strategy.name());
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
            updateReadAndDeliveredStatus(notification, response);
        }
    }

    @Override
    public void sendPrioritySmsNotification(Message message) {
        Notification notification = (Notification) message.getPayload();
        defaultSmsSender.sendPrioritySMS(notification.getMessage(), notification.getTarget().getPhoneNumber());
    }

    @Override
    public void sendSmsNotificationOnError(Message message) {

    }

    private void updateReadAndDeliveredStatus(Notification notification, SmsGatewayResponse response) {
        notificationBroker.markNotificationAsDelivered(notification.getUid());
        if (response.isSuccessful()) {
            notificationBroker.updateNotificationReadStatus(notification.getUid(), true);
        } else {
            switch (response.getResponseType()) {
                case MSISDN_INVALID: // todo : record / process / somewhere & somehow
                    logger.error("invalid number for SMS, marking it as read to prevent looping redelivery");
                    notificationBroker.updateNotificationReadStatus(notification.getUid(), true); // to prevent unread trying to send
                    break;
                case DUPLICATE_MESSAGE:
                    logger.error("trying to resend message, just set it as read");
                    notificationBroker.updateNotificationReadStatus(notification.getUid(), true); // as above, prevents loops
                    break;
                default:
                    logger.error("error delivering SMS, response from gateway: {}", response.toString());
            }
        }
    }
}