package za.org.grassroot.messaging.service.gcm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by paballo on 2016/04/05.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcmPayload {

    private String from;

    @JsonProperty("message_id")
    private String messageId;

    private String to;

    @JsonProperty("collapse_key")
    private String collapseKey;
    @JsonProperty("delay_while_idle")
    private boolean delayWhileIdle = false;
    @JsonProperty("delivery_receipt_requested")
    private boolean deliveryReceiptRequested =true;

    private final String priority = "high";

    private Map<String,Object> notification;

    private Map<String,Object> data;


    @JsonProperty("message_type")
    private String messageType;

    public GcmPayload() {
        // for Object Mapper
    }

    public GcmPayload(String messageId, String to, String messageType) {
        this.messageId = messageId;
        this.to = to;
        this.messageType = messageType;
    }

    public GcmPayload(String messageId, String to, String collapseKey, String dataBody) {
        this(messageId, to, null);
        this.collapseKey = collapseKey;
        this.data = new HashMap<>();
        this.data.put("body", dataBody);
    }


    public String getMessageId() {
        return messageId;
    }

    public String getFrom() {
        return from;
    }

    public Map<String,Object> getNotification() {
        return notification;
    }

    public String getTo() {
        return to;
    }

    public String getCollapseKey() {
        return collapseKey;
    }

    public boolean isDelayWhileIdle() {
        return delayWhileIdle;
    }

    public boolean isDeliveryReceiptRequested() {
        return deliveryReceiptRequested;
    }

    public String getPriority() {
        return priority;
    }

    public Map<String,Object> getData() {
        return data;
    }

    public String getMessageType() {
        return messageType;
    }


    @Override
    public String toString() {
        return "GcmPayload{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", messageType='" + messageType + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
