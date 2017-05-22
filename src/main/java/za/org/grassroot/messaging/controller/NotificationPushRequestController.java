package za.org.grassroot.messaging.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import za.org.grassroot.messaging.service.MessageSendingService;
import za.org.grassroot.messaging.service.jwt.JwtService;
import za.org.grassroot.messaging.service.sms.model.MockSmsResponse;
import za.org.grassroot.messaging.service.sms.model.SmsGatewayResponse;

/**
 * Created by luke on 2017/05/20.
 * Used by main platform (or others in future) to trigger a priority SMS
 */
@RestController
@RequestMapping(value = "/notification/push")
public class NotificationPushRequestController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPushRequestController.class);

    private final MessageSendingService messageSendingService;

    @Autowired
    public NotificationPushRequestController(JwtService jwtService, MessageSendingService messageSendingService) {
        super(jwtService);
        this.messageSendingService = messageSendingService;
    }

    @RequestMapping(value = "/priority/{phoneNumber}", method = RequestMethod.POST)
    public @ResponseBody SmsGatewayResponse triggerPriorityNotification(@PathVariable String phoneNumber,
                                                   @RequestParam String message,
                                                   @RequestParam(required = false) Integer priorityLevel) {
        // messageSendingService.sendPriorityMessage(phoneNumber, message, priorityLevel == null ? 0 : priorityLevel);
        logger.info("Sending priority SMS! to : {}", phoneNumber);
        return new MockSmsResponse();
    }

}
