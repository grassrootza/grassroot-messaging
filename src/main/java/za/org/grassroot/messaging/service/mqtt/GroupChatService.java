package za.org.grassroot.messaging.service.mqtt;

import za.org.grassroot.messaging.domain.GroupChatSettings;

import java.util.List;
import java.util.Set;

/**
 * Created by paballo on 2016/09/08.
 */
public interface GroupChatService {

    void processCommandMessage(MQTTPayload incoming);

    void markMessagesAsRead(String groupUid, String groupName, Set<String> messageUids);

    GroupChatSettings load(String userUid, String groupUid);

    void createUserGroupMessagingSetting(String userUid, String groupUid, boolean active, boolean canSend, boolean canReceive);

    void updateActivityStatus(String userUid, String groupUid, boolean active, boolean userInitiated) throws Exception;

    void createGroupChatMessageStats(MQTTPayload payload);

    List<String> usersMutedInGroup(String groupUid);

}