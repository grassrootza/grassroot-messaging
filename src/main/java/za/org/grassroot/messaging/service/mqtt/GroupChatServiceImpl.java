package za.org.grassroot.messaging.service.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.domain.Group;
import za.org.grassroot.core.domain.GroupChatSettings;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.enums.TaskType;
import za.org.grassroot.core.repository.GroupChatSettingsRepository;
import za.org.grassroot.core.repository.GroupRepository;
import za.org.grassroot.core.repository.UserRepository;
import za.org.grassroot.messaging.domain.GroupChatMessageStats;
import za.org.grassroot.messaging.domain.exception.SeloParseDateTimeFailure;
import za.org.grassroot.messaging.domain.repository.GroupChatStatsRepository;
import za.org.grassroot.messaging.service.LearningService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by paballo on 2016/09/08.
 */
@Service
@ConditionalOnProperty(name = "grassroot.mqtt.enabled", havingValue = "true",  matchIfMissing = false)
public class GroupChatServiceImpl implements GroupChatService {

    private static final Logger logger = LoggerFactory.getLogger(GroupChatServiceImpl.class);
    private static final DateTimeFormatter cmdMessageFormat = DateTimeFormatter.ofPattern("HH:mm, EEE d MMM");

    @Value("${mqtt.status.read.threshold:0.5}")
    private Double readStatusThreshold;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupChatSettingsRepository groupChatSettingsRepository;
    private final GroupChatStatsRepository groupChatMessageStatsRepository;

    private final LearningService learningService;
    private final MessageSourceAccessor messageSourceAccessor;

    private ObjectMapper payloadMapper;
    private MessageChannel mqttOutboundChannel;
    private MqttPahoMessageDrivenChannelAdapter mqttAdapter;

    @Autowired
    public GroupChatServiceImpl(UserRepository userRepository, GroupRepository groupRepository, GroupChatSettingsRepository groupChatSettingsRepository,
                                LearningService learningService, GroupChatStatsRepository groupChatMessageStatsRepository,
                                MessageSource messageSource) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupChatSettingsRepository = groupChatSettingsRepository;
        this.learningService = learningService;
        this.groupChatMessageStatsRepository = groupChatMessageStatsRepository;
        this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
    }

    @Autowired
    private void setMqttOutboundChannel(MessageChannel mqttOutboundChannel) {
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    @Autowired
    private void setPayloadMapper(@Qualifier("mqttObjectMapper") ObjectMapper payloadMapper) {
        this.payloadMapper = payloadMapper;
    }

    @Autowired
    private void setMqttAdapter(MqttPahoMessageDrivenChannelAdapter mqttAdapter) {
        this.mqttAdapter = mqttAdapter;
    }

    @Scheduled(fixedRate = 300000)
    public void reactivateMutedUsers() throws Exception {
        logger.info("Reactivating muted group users ...");
        List<GroupChatSettings> groupChatSettingses = groupChatSettingsRepository
                .findByActiveFalseAndUserInitiatedFalseAndReactivationTimeBefore(Instant.now());
        for (GroupChatSettings messengerSetting : groupChatSettingses) {
            String userUid = messengerSetting.getUser().getUid();
            String groupUid = messengerSetting.getGroup().getUid();
            try {
                updateActivityStatus(userUid, groupUid, true, false);
            } catch (IllegalArgumentException e) {
                logger.error("Error while trying un-mute user with " + userUid);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * *") //runs at 1 am everyday
    public void subscribeServerToAllGroupTopics() {
        logger.info("Subscribing server to all group topics");
        List<Group> groups = groupRepository.findAll();
        List<String> topicsSubscribedTo = Arrays.asList(mqttAdapter.getTopic());
        for(Group group: groups){
            if(!topicsSubscribedTo.contains(group.getUid())){
                mqttAdapter.addTopic(group.getUid(), 1);
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    public void sendPollingMessage(){
        if (mqttAdapter != null) {
            List<String> topicsSubscribedTo = Arrays.asList(mqttAdapter.getTopic());
            if (!topicsSubscribedTo.contains("Grassroot")) {
                mqttAdapter.addTopic("Grassroot", 1);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "groupChatSettings", key = "userUid + '_'+ groupUid")
    public GroupChatSettings load(String userUid, String groupUid) {
        Objects.requireNonNull(userUid);
        Objects.requireNonNull(groupUid);

        User user = userRepository.findOneByUid(userUid);
        Group group = groupRepository.findOneByUid(groupUid);

        GroupChatSettings groupChatSettings = groupChatSettingsRepository.findTopByUserAndGroupOrderByCreatedDateTimeDesc(user, group);

        if (groupChatSettings == null) {
            throw new IllegalArgumentException("Group chat setting not found found for user with uid " + userUid);
        }

        return groupChatSettings;
    }


    @Override
    public void processCommandMessage(MQTTPayload incoming) {
        Group group = groupRepository.findOneByUid(incoming.getGroupUid());
        MQTTPayload payload = generateCommandResponseMessage(incoming, group);
        try {
            final String message = payloadMapper.writeValueAsString(payload);
            mqttOutboundChannel.send(MessageBuilder.withPayload(message).
                    setHeader(MqttHeaders.TOPIC, incoming.getPhoneNumber()).build());
        } catch (JsonProcessingException e) {
            // todo : send back a "sorry we couldn't handle it" message
            logger.debug("Message conversion failed with error ={}", e.getMessage());
        }

    }

    @Override
    @Transactional
    public void markMessagesAsRead(String groupUid, Set<String> messageUids) {
        messageUids.forEach(u -> {
            GroupChatMessageStats stats = groupChatMessageStatsRepository.findByMessageUidAndReadFalse(u);
            if (stats != null) {
                stats.incrementReadCount();
                checkForReadThresholdAndNotify(stats);
            }
        });
    }

    private void checkForReadThresholdAndNotify(GroupChatMessageStats stats) {
        if (stats.getTimesRead() / stats.getIntendedReceipients() > readStatusThreshold) {
            MQTTPayload payload = new MQTTPayload(stats.getMessageUid(),
                    stats.getGroup().getUid(),
                    stats.getGroup().getGroupName(),
                    "Grassroot",
                    "update_read_status");
            stats.setRead(true);
            try {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
                final String message = mapper.writeValueAsString(payload);
                mqttOutboundChannel.send(MessageBuilder
                        .withPayload(message)
                        .setHeader(MqttHeaders.TOPIC, stats.getUser().getPhoneNumber())
                        .build());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @Transactional
    public void updateActivityStatus(String userUid, String groupUid, boolean active, boolean userInitiated) {
        GroupChatSettings groupChatSettings = load(userUid, groupUid);
        User user = userRepository.findOneByUid(userUid);
        Group group = groupRepository.findOneByUid(groupUid);

        groupChatSettings.setActive(active);
        groupChatSettings.setUserInitiated(userInitiated);
        groupChatSettings.setCanSend(active);

        if (userInitiated) {
            groupChatSettings.setCanReceive(active);
        }

        if(!userInitiated && !active){
            try {
                final MQTTPayload payload = generateUserMutedResponseData(group);
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,true);
                mqttOutboundChannel.send(MessageBuilder
                        .withPayload(mapper.writeValueAsString(payload))
                        .setHeader(MqttHeaders.TOPIC, user.getPhoneNumber())
                        .build());
            } catch (JsonProcessingException e) {
                logger.debug("Error parsing message");
            }

        }
    }

    @Override
    @Transactional
    public void createGroupChatMessageStats(MQTTPayload payload) {
        Objects.requireNonNull(payload);
        Group group = groupRepository.findOneByUid(payload.getGroupUid());
        User user = userRepository.findByPhoneNumber(payload.getPhoneNumber());
        if(group !=null && user != null) {
            Long numberOfIntendedRecepients = groupChatSettingsRepository.countByGroupAndActiveTrue(group);
            GroupChatMessageStats groupChatMessageStats = new GroupChatMessageStats(payload.getUid(), group, user, numberOfIntendedRecepients, 1L, false);
            groupChatMessageStatsRepository.save(groupChatMessageStats);
        }
    }

    @Override
    public void subscribeServerToGroupTopic(String groupUid) {
        Objects.requireNonNull(groupUid);
        List<String> topicsSubscribeTo = Arrays.asList(mqttAdapter.getTopic());
        if (!topicsSubscribeTo.contains(groupUid)) {
            mqttAdapter.addTopic(groupUid, 1);
        }
    }

    private MQTTPayload generateInvalidCommandResponseData(MQTTPayload input, Group group) {
        String responseMessage = messageSourceAccessor.getMessage("gcm.xmpp.command.invalid");
        MQTTPayload outboundMessage = new MQTTPayload(input.getUid(),
                input.getGroupUid(),
                group.getGroupName(),
                "Grassroot",
                "error");
        outboundMessage.setText(responseMessage);
        return outboundMessage;
    }

    private MQTTPayload generateDateInPastResponse(MQTTPayload input, Group group) {
        String responseMessage = messageSourceAccessor.getMessage("gcm.xmpp.command.timepast");
        MQTTPayload outboundMessage = new MQTTPayload(input.getUid(),
                input.getGroupUid(),
                group.getGroupName(),
                "Grassroot",
                "sync");
        outboundMessage.setText(responseMessage);
        return outboundMessage;
    }

    private MQTTPayload generateCommandResponseData(MQTTPayload input, Group group, TaskType type, String[] tokens,
                                                    LocalDateTime taskDateTime) {

        MQTTPayload outboundMessage = new MQTTPayload(input.getUid(),
                input.getGroupUid(),
                group.getGroupName(),
                "Grassroot",
                LocalDateTime.now(),
                taskDateTime,
                "SERVER_PROMPT");

        if (TaskType.MEETING.equals(type)) {
            final String text = messageSourceAccessor.getMessage("gcm.xmpp.command.meeting", tokens);
            outboundMessage.setText(text);
            outboundMessage.setTaskType(TaskType.MEETING.name());
        } else if (TaskType.VOTE.equals(type)) {
            final String text = messageSourceAccessor.getMessage("gcm.xmpp.command.vote", tokens);
            outboundMessage.setText(text);
            outboundMessage.setTaskType(TaskType.VOTE.name());
        } else {
            final String text = messageSourceAccessor.getMessage("gcm.xmpp.command.todo", tokens);
            outboundMessage.setText(text);
            outboundMessage.setTaskType(TaskType.TODO.name());
        }

        outboundMessage.setTokens(Arrays.asList(tokens));
        return outboundMessage;
    }


    private MQTTPayload generateCommandResponseMessage(MQTTPayload input, Group group) {
        MQTTPayload data;
        final String msg = input.getText();
        final String[] tokens = splitCommandMessage(msg);

        final TaskType cmdType = msg.contains("/meeting") ? TaskType.MEETING :
                msg.contains("/vote") ? TaskType.VOTE : TaskType.TODO;

        if (tokens.length < (TaskType.MEETING.equals(cmdType) ? 3 : 2)) {
            data = generateInvalidCommandResponseData(input, group);
        } else {
            try {
                final LocalDateTime parsedDateTime = learningService.parse(tokens[1]);
                ZonedDateTime zonedDateTime = ZonedDateTime.of(parsedDateTime, ZoneId.of("Africa/Johannesburg"));
                if (zonedDateTime.isBefore(ZonedDateTime.now())) {
                    logger.info("time is in the past");
                    data = generateDateInPastResponse(input, group);
                } else {
                    tokens[1] = parsedDateTime.format(cmdMessageFormat);
                    data = generateCommandResponseData(input, group, cmdType, tokens, parsedDateTime);
                }
            } catch (SeloParseDateTimeFailure e) {
                data = generateInvalidCommandResponseData(input, group);
            }
        }

        return data;
    }

    private MQTTPayload generateUserMutedResponseData(Group group) {
        String groupUid = group.getUid();
        String messageId = UUID.randomUUID().toString();
        String responseMessage = messageSourceAccessor.getMessage("gcm.xmpp.chat.muted");
        MQTTPayload payload =  new MQTTPayload(messageId,
                groupUid,
                group.getGroupName(),
                group.getGroupName(),
                "normal");
        payload.setText(responseMessage);

        return payload;
    }

    private String[] splitCommandMessage(String message) {
        if (message.contains("/meeting")) {
            message = message.replace("/meeting", "");
        }
        if (message.contains("/vote")) {
            message = message.replace("/vote", "");
        }
        if (message.contains("/todo")) {
            message = message.replace("/todo", "");
        }
        return message.split(",");
    }

}