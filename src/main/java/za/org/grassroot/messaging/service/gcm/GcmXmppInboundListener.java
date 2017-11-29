package za.org.grassroot.messaging.service.gcm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.messaging.service.NotificationBroker;

/**
 * Created by luke on 2017/05/19.
 */
@Component
@ConditionalOnProperty(value = "grassroot.gcm.enabled", havingValue = "true")
public class GcmXmppInboundListener implements StanzaListener {

    private static final Logger logger = LoggerFactory.getLogger(GcmXmppInboundListener.class);
    private final ObjectMapper objectMapper;
    private final NotificationBroker notificationBroker;
    private final GcmHandlingBroker gcmHandlingBroker;

    @Autowired
    public GcmXmppInboundListener(NotificationBroker notificationBroker, GcmHandlingBroker gcmHandlingBroker,
                                  XMPPConnection xmppConnection, @Qualifier("gcmObjectMapper") ObjectMapper objectMapper) {
        this.notificationBroker = notificationBroker;
        this.gcmHandlingBroker = gcmHandlingBroker;
        this.objectMapper = objectMapper;

        xmppConnection.addSyncStanzaListener(this, new StanzaTypeFilter(Message.class));
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        if (packet instanceof Message) {
            try {
                GcmPacketExtension gcmPacket = GcmPacketExtension.from(packet);
                GcmPayload gcmPayload = objectMapper.readValue(gcmPacket.getJson(), GcmPayload.class);
                String messageType = gcmPayload.getMessageType();

                if (StringUtils.isEmpty(messageType)) {
                    handleUpstreamMessage(gcmPayload);
                } else {
                    switch (messageType) {
                        case "ack":
                            logger.debug("Gcm acknowledges receipt of message {}, with payload {}", gcmPayload.getMessageId(), gcmPayload.getData());
                            break;
                        case "nack":
                            handleNotAcknowledged(gcmPayload);
                            break;
                        case "receipt":
                            // not using this since it proved to be unreliable, using custom upstream message for delivery receipt
                            break;
                        case "control":
                            break;
                        default:
                            logger.warn("Received unknown gcm message type");
                            break;
                    }
                }
            } catch(JsonParseException|JsonMappingException e) {
                logger.warn("Error! Could not parse or map inbound packet");
                e.printStackTrace();
            } catch (Exception e) {
                logger.warn("Error! Could not process inbound packet");
                e.printStackTrace();
            }
        } else {
            logger.warn("Warning! Received unknown message type over XMPP");
        }
    }

    private void handleUpstreamMessage(GcmPayload message) {
        String messageId = message.getMessageId();
        String from = message.getFrom();

        String action = String.valueOf(message.getData().get("action"));
        if (action != null) {
            switch (action) {
                case "REGISTER":
                    String phoneNumber = (String) message.getData().get("phoneNumber");
                    gcmHandlingBroker.registerUser(phoneNumber, from);
                    break;
                case "DELIVERY_RECEIPT":
                    String deliveredMsgId = String.valueOf(message.getData().get("delivered_message_id"));
                    handleDeliveryReceipt(deliveredMsgId);
                    break;
                case "UPDATE_READ":
                    String notificationId = (String) message.getData().get("notificationId");
                    notificationBroker.updateNotificationStatus(notificationId, NotificationStatus.READ, null, false, null, null);
                    break;
                default: //action unknown ignore
                    break;
            }
        }
        gcmHandlingBroker.sendGcmAcknowledgement(from, messageId);
    }

    private void handleNotAcknowledged(GcmPayload payload) {
        Notification notification = notificationBroker.loadNotification(payload.getMessageId());
        if (notification != null) {
            logger.info("Push Notification delivery failed, should now send SMS to  {}", notification.getTarget().getPhoneNumber());
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED,
                    "Push notification delivery failed!", false, null, null);
        } else {
            logger.debug("Received an upstream message without notification, looks like: {}", payload);
        }
    }

    private void handleDeliveryReceipt(String messageId) {
        logger.debug("Message " + messageId + " delivery successful, updating notification to delivered status.");
        notificationBroker.updateNotificationStatus(messageId, NotificationStatus.DELIVERED, null, false, null, null);
    }

}
