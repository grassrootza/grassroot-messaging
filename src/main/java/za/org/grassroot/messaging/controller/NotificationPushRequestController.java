package za.org.grassroot.messaging.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.org.grassroot.messaging.service.MessageSendingService;
import za.org.grassroot.messaging.service.jwt.JwtService;

/**
 * Created by luke on 2017/05/20.
 * Used by main platform (or others in future) to trigger a priority SMS
 */
@RestController
@RequestMapping(value = "/notification/push")
public class NotificationPushRequestController extends BaseController {

    private final MessageSendingService messageSendingService;

    @Autowired
    public NotificationPushRequestController(JwtService jwtService, MessageSendingService messageSendingService) {
        super(jwtService);
        this.messageSendingService = messageSendingService;
    }

    @RequestMapping(value = "/priority/{phoneNumber}")
    public ResponseEntity<?> triggerPriorityNotification(@PathVariable String phoneNumber,
                                                         @RequestParam String message,
                                                         @RequestParam(required = false) Integer priorityLevel) {
        messageSendingService.sendPriorityMessage(phoneNumber, message, priorityLevel == null ? 0 : priorityLevel);
        return ResponseEntity.ok().build();
    }

}
