package za.org.grassroot.messaging.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.org.grassroot.messaging.service.jwt.JwtService;

/**
 * Created by luke on 2017/05/20.
 * To trigger request to subscribe server to an MQTT topic, etc
 */
@RestController
@RequestMapping("/groupchat/")
public class GroupChatRequestController extends BaseController {

    @Autowired
    public GroupChatRequestController(JwtService jwtService) {
        super(jwtService);
    }
}
