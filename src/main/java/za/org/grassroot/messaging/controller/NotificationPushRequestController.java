package za.org.grassroot.messaging.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.*;
import za.org.grassroot.messaging.domain.MessageAndRoutingBundle;
import za.org.grassroot.messaging.domain.PriorityMessage;
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

    private MessageChannel requestChannel;
    private MessageChannel priorityChannel;

    @Autowired
    public NotificationPushRequestController(JwtService jwtService) {
        super(jwtService);
    }

    @Autowired
    public void setRequestChannel(@Qualifier("outboundSystemChannel") MessageChannel requestChannel) {
        this.requestChannel = requestChannel;
    }

    @Autowired
    public void setPriorityChannel(@Qualifier("outboundPriorityChannel") MessageChannel priorityChannel) {
        this.priorityChannel = priorityChannel;
    }

    @RequestMapping(value = "/system/{phoneNumber}", method = RequestMethod.POST)
    public @ResponseBody SmsGatewayResponse sendSystemMessage(@PathVariable String phoneNumber,
                                                                     @RequestParam String message,
                                                                     @RequestParam(required = false) Boolean userRequested) {
        logger.info("Sending normal system SMS to {}", phoneNumber);
        requestChannel.send(MessageBuilder
                .withPayload(new MessageAndRoutingBundle(phoneNumber, message, userRequested)).build());
        return new MockSmsResponse();
    }

    @RequestMapping(value = "/priority/{phoneNumber}", method = RequestMethod.POST)
    public @ResponseBody SmsGatewayResponse triggerPriorityNotification(@PathVariable String phoneNumber,
                                                   @RequestParam String message,
                                                   @RequestParam(required = false) Integer priorityLevel) {
        logger.info("Sending priority SMS! to : {}", phoneNumber);
        priorityChannel.send(MessageBuilder
                .withPayload(new PriorityMessage(phoneNumber, message))
                .setHeader("route", "PRIORITY")
                .setHeader("priority", priorityLevel == null ? 0 : priorityLevel)
                .build());
        return new MockSmsResponse();
    }

}
