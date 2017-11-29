package za.org.grassroot.messaging.service.mqtt;

import java.util.Set;

/**
 * Created by paballo on 2016/09/08.
 */
public interface GroupChatService {

    void processCommandMessage(MQTTPayload incoming);

    void markMessagesAsRead(String groupUid, Set<String> messageUids);

    void subscribeServerToGroupTopic(String groupUid);

}