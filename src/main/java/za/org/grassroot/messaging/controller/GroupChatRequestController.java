package za.org.grassroot.messaging.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.org.grassroot.messaging.service.jwt.JwtService;
import za.org.grassroot.messaging.service.mqtt.GroupChatService;

import java.util.Set;

/**
 * Created by luke on 2017/05/20.
 * To trigger request to subscribe server to an MQTT topic, etc
 */
@RestController
@RequestMapping("/groupchat/")
@ConditionalOnProperty(name = "grassroot.mqtt.enabled", havingValue = "true",  matchIfMissing = false)
public class GroupChatRequestController extends BaseController {

    private final GroupChatService groupChatService;

    @Autowired
    public GroupChatRequestController(JwtService jwtService, GroupChatService groupChatService) {
        super(jwtService);
        this.groupChatService = groupChatService;
    }

    @RequestMapping(value = "/mark_read/{groupUid}", method = RequestMethod.POST)
    public ResponseEntity markMessagesRead(@PathVariable String groupUid, Set<String> messageUids) {
        groupChatService.markMessagesAsRead(groupUid, messageUids);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/update_activity/{userUid}", method = RequestMethod.POST)
    public ResponseEntity updateActivityStatus(@PathVariable String userUid, @RequestParam String groupUid,
                                               @RequestParam boolean setActive, @RequestParam boolean selfInitiated) {
        groupChatService.updateActivityStatus(userUid, groupUid, setActive, selfInitiated);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/server_subscribe/{groupUid}", method = RequestMethod.POST)
    public ResponseEntity subscribeServerToGroupTopic(@PathVariable String groupdUid) {
        groupChatService.subscribeServerToGroupTopic(groupdUid);
        return ResponseEntity.ok().build();
    }


}
