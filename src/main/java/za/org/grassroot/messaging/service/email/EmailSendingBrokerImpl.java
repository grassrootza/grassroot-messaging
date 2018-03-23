package za.org.grassroot.messaging.service.email;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.util.Duration;
import com.sendgrid.*;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.org.grassroot.core.GrassrootTemplate;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.domain.media.MediaFileRecord;
import za.org.grassroot.core.domain.notification.EventNotification;
import za.org.grassroot.core.domain.notification.TodoNotification;
import za.org.grassroot.core.domain.task.Event;
import za.org.grassroot.core.domain.task.Todo;
import za.org.grassroot.core.dto.GrassrootEmail;
import za.org.grassroot.core.enums.EventType;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.enums.Province;
import za.org.grassroot.core.repository.MediaFileRecordRepository;
import za.org.grassroot.core.repository.UserRepository;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.service.StorageBroker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service @Slf4j
@ConditionalOnProperty(value = "grassroot.email.enabled", havingValue = "true")
public class EmailSendingBrokerImpl implements EmailSendingBroker {

    private static final DateTimeFormatter SDF = DateTimeFormatter.ofPattern("EEE d MMM");
    private static final String NO_PROVINCE = "your province";

    private static final Pattern imgRegExp  = Pattern.compile( "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>" );
    private static final String srcToken = "src=\"";

    @Value("${SENDGRID_API_KEY:testing}")
    private String sendGridApiKey;

    @Value("${grassroot.email.test.enabled:true}")
    private boolean routeMailsToTest;

    @Value("${grassroot.email.test.address:test@grassroot.org.za}")
    private String testEmailAddress;

    @Value("${grassroot.email.test.sandbox:false}")
    private boolean useSandboxOnly;

    @Value("${grassroot.notifications.email.from:notifications@grassroot.org.za}")
    private String fromAddress;

    @Value("${grassroot.email.default.name:Grassroot}")
    private String defaultFromName;

    private static final String DEFAULT_SUBJECT = "Grassroot notification";
    private static final String NOTIFICATION_BODY_HTML = "<p>Dear %1$s</p><p><b>Notice: </b>%2$s</p><p>%3$s</p>";

    private static final String NOTIFICATION_BODY_TEXT = "Dear %1$s,\n\n%2$s\n\n%3$s";
    private static final String NOTIFICATION_FOOTER_PLAIN = "Sent by Grassroot";
    private static final String NOTIFICATION_FOOTER_ACCOUNT = "Sent for %1$s by Grassoot";

    private final NotificationBroker notificationBroker;
    private final MediaFileRecordRepository recordRepository;
    private final StorageBroker storageBroker;
    private final UserRepository userRepository;

    public EmailSendingBrokerImpl(NotificationBroker notificationBroker, MediaFileRecordRepository recordRepository, StorageBroker storageBroker, UserRepository userRepository) {
        this.notificationBroker = notificationBroker;
        this.recordRepository = recordRepository;
        this.storageBroker = storageBroker;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void sendNotificationByEmail(Message message) {
        Notification notification = (Notification) message.getPayload();
        notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENDING, null, true, false, null,
                MessagingProvider.EMAIL);

        try {
            Response response = sendMail(mailFromNotification(notification));
            if (response.getStatusCode() / 100 == 2) {
                notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENT, null,
                        true, false, null, MessagingProvider.EMAIL);
            } else {
                updateNotificationFailed(notification.getUid(), response.getBody());
            }
        } catch (IOException e) {
            log.error("Error sending a notification mail", e);
            updateNotificationFailed(notification.getUid(), "Unknown IO error sending message via email");
        }
    }

    @Async
    @Override
    public void sendNonNotificationEmails(Set<GrassrootEmail> emails) {
        log.info("trying to send out a list of {} emails", emails.size());
        emails.stream()
                .map(this::transformEmail)
                .filter(Objects::nonNull)
                .forEach(mail -> {
                    try {
                        sendMail(mail);
                    } catch (IOException e) {
                        log.error("Error inside java mail sending loop", e);
                    }
                });
        log.info("Sent emails!");
    }

    private Response sendMail(Mail mail) throws IOException {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");

        request.setBody(mail.build());
        log.info("sendgrid: request params: {}, and body: {}", request.getQueryParams(), request.getBody());
        Response response = sg.api(request);
        log.info("sendgrid: here is our status code: {}, and headers: {}", response.getStatusCode(), response.getHeaders());
        return response;
    }

    private Mail checkForSandbox(Mail mail) {
        if (useSandboxOnly) {
            Setting sandboxMode = new Setting();
            sandboxMode.setEnable(true);
            MailSettings settings = new MailSettings();
            settings.setSandboxMode(sandboxMode);
            mail.setMailSettings(settings);
        }
        return mail;
    }

    private Mail mailFromNotification(Notification notification) {
        Mail mail = new Mail();

        User sender = notification.getSender();
        Email from;
        Email replyTo;
        if (sender != null && sender.hasEmailAddress()) {
            from = new Email(sender.getEmailAddress(), sender.getName());
            replyTo = new Email(sender.getEmailAddress(), sender.getName());
        } else {
            from = new Email(fromAddress, defaultFromName);
            replyTo = new Email(fromAddress);
        }

        mail.setFrom(from);
        mail.setReplyTo(replyTo);

        String subject;
        if (notification instanceof EventNotification) {
            Event event = ((EventNotification) notification).getEvent();
            subject = event.getAncestorGroup().getName() + ": " + event.getName();
            mail.addCustomArg("event_id", event.getUid());
        } else if (notification instanceof TodoNotification) {
            Todo todo = ((TodoNotification) notification).getTodo();
            subject = todo.getAncestorGroup().getName() + ": " + todo.getName();
            mail.addCustomArg("todo_id", todo.getUid());
        } else {
            subject = DEFAULT_SUBJECT;
        }

        mail.setSubject(subject);

        User target = notification.getTarget();
        mail.addPersonalization(userBasedPersonalization(target, notification.getUid()));

        final String footer = sender == null || sender.getPrimaryAccount() == null ? NOTIFICATION_FOOTER_PLAIN :
                String.format(NOTIFICATION_FOOTER_ACCOUNT, sender.getName());
        final String bodyPlain = String.format(NOTIFICATION_BODY_TEXT, target.getName(), notification.getMessage(), footer);
        final String bodyHtml = String.format(NOTIFICATION_BODY_HTML, target.getName(), notification.getMessage(), footer);

        Content textContent = new Content("text/plain", bodyPlain);
        Content htmlContent = new Content("text/html", bodyHtml);
        mail.addContent(textContent);
        mail.addContent(htmlContent);

        InputStream calendarAttachment = notification instanceof EventNotification ? calendarAttachment((EventNotification) notification) : null;
        log.debug("do we have a calendar attachment? : {}", calendarAttachment);
        if (calendarAttachment != null) {
            Attachments attachments = new Attachments.Builder("meeting.ics", calendarAttachment).build();
            mail.addAttachments(attachments);
        }

        mail.addCustomArg("notification_uid", notification.getUid());

        return checkForSandbox(mail);
    }

    private void updateNotificationFailed(String notificationUid, String cause) {
        notificationBroker.updateNotificationStatus(notificationUid, NotificationStatus.DELIVERY_FAILED,
                "Error sending message via email", true, false, null, MessagingProvider.EMAIL);
    }

    private InputStream calendarAttachment(EventNotification notification) {
        ICalendar ical = new ICalendar();
        VEvent virtualEvent = new VEvent();
        Event event = notification.getEvent();
        if (event == null || EventType.VOTE.equals(event.getEventType())) {
            return null;
        }

        virtualEvent.setSummary(event.getName());
        virtualEvent.setDescription(notification.getEvent().getDescription());
        Date start = Date.from(event.getEventStartDateTime());
        virtualEvent.setDateStart(start);
        Duration duration = new Duration.Builder().hours(1).build();
        virtualEvent.setDuration(duration);
        ical.addEvent(virtualEvent);

        try {
            return new ByteArrayResource(Biweekly.write(ical).go().getBytes(StandardCharsets.UTF_8.name())).getInputStream();
        } catch (IOException e) {
            log.error("Something went wrong writing to iCal", e);
            return null;
        }
    }


    private Mail transformEmail(GrassrootEmail email) {
        Mail mail = new Mail();
        mail.setSubject(email.getSubject());

        boolean hasFromName = !StringUtils.isEmpty(email.getFromName());
        boolean hasFromAddress = !StringUtils.isEmpty(email.getFromAddress());
        Email fromPerson = hasFromName ? new Email(hasFromAddress ? email.getFromAddress() : fromAddress,
                email.getFromName()) : new Email(hasFromAddress ? email.getFromAddress() : fromAddress);
        mail.setFrom(fromPerson);

        if (hasFromAddress || hasFromName) {
            Email replyTo = !hasFromName ? new Email(email.getFromAddress()) : new Email(email.getFromAddress(), email.getFromName());
            mail.setReplyTo(replyTo);
        }

        if (email.isMultiUser()) {
            log.info("generating personalization, email base ID = {}", email.getBaseId());
            email.getToUserUids().stream().map(userRepository::findOneByUid)
                    .map(user -> userBasedPersonalization(user, email.getBaseId()))
                    .forEach(mail::addPersonalization);
        } else {
            mail.addPersonalization(nameBasedPersonalization(email.getToName(), email.getToAddress(), email.getBaseId()));
        }

        if (email.hasHtmlContent()) {
            List<InlineImage> imgMap = new ArrayList<>();
            String htmlContent = searchForImagesInHtml(email.getHtmlContent(), imgMap);
            log.debug("traversed, image map = {}, htmlContent = {}", imgMap, htmlContent);
            // also just to remove the image, in case it's there
            final String text = searchForImagesInHtml(email.getContent(), new ArrayList<>());
            mail.addContent(new Content("text/html", text));
            List<Attachments> attachments = imgMap.stream().map(this::addInlineImage).collect(Collectors.toList());
            safeAddAttachments(mail, attachments);
        } else {
            mail.addContent(new Content("text/plain", email.getContent()));
        }

        if (email.hasAttachment()) {
            mail = safeAddAttachments(mail, Collections.singleton(createAttachment(email.getAttachmentName(), email.getAttachment(), null)));
        }

        List<Attachments> attachments = email.getAttachmentUidsAndNames().entrySet().stream().map(entry -> {
            MediaFileRecord record = recordRepository.findOneByUid(entry.getKey());
            File fileToAttach = storageBroker.fetchFileFromRecord(record);
            final String attachmentName = !StringUtils.isEmpty(entry.getValue()) ? entry.getValue() :
                    !StringUtils.isEmpty(record.getFileName()) ? record.getFileName() : fileToAttach.getName();
            return createAttachment(attachmentName, fileToAttach, record.getMimeType());
        }).collect(Collectors.toList());

        if (!attachments.isEmpty()) {
            mail = safeAddAttachments(mail, attachments);
        }

        return checkForSandbox(mail);
    }

    private Personalization userBasedPersonalization(User user, String baseId) {
        Personalization personalization = new Personalization();

        final String emailAddress = routeMailsToTest ? testEmailAddress : user.getEmailAddress();
        Email toPerson = user.hasName() ? new Email(emailAddress, user.getName()) : new Email(emailAddress);

        personalization.addTo(toPerson);
        personalization.addCustomArg("base_id", baseId);
        personalization.addCustomArg("user_id", user.getUid());

        personalization.addSubstitution(GrassrootTemplate.NAME_FIELD_TEMPLATE, user.getName());
        personalization.addSubstitution(GrassrootTemplate.DATE_FIELD_TEMPLATE, SDF.format(LocalDate.now()));
        personalization.addSubstitution(GrassrootTemplate.CONTACT_FIELD_TEMPALTE, user.getUsername());
        personalization.addSubstitution(GrassrootTemplate.PROVINCE_FIELD_TEMPLATE, user.getProvince() == null ?
                NO_PROVINCE : Province.CANONICAL_NAMES_ZA.getOrDefault(user.getProvince(), NO_PROVINCE));

        if (routeMailsToTest) {
            personalization.addHeader("X-Test", "test");
        }

        return personalization;
    }

    private Personalization nameBasedPersonalization(String toName, String toAddress, String baseId) {
        Personalization personalization = new Personalization();

        final boolean hasName = !StringUtils.isEmpty(toName);
        final String emailAddress = routeMailsToTest ? testEmailAddress : toAddress;

        Email toPerson = hasName ? new Email(emailAddress, toName) : new Email(emailAddress);
        personalization.addTo(toPerson);

        if (hasName) {
            personalization.addSubstitution(GrassrootTemplate.NAME_FIELD_TEMPLATE, toName);
            personalization.addSubstitution(GrassrootTemplate.DATE_FIELD_TEMPLATE, SDF.format(LocalDate.now()));
        }

        if (!StringUtils.isEmpty(baseId)) {
            personalization.addCustomArg("base_id", baseId);
        }

        if (routeMailsToTest) {
            personalization.addHeader("X-Test", "test");
        }

        return personalization;
    }

    private Attachments createAttachment(String name, File file, String type) {
        try {
            InputStream stream = FileUtils.openInputStream(file);
            Attachments.Builder builder = new Attachments
                    .Builder(name, stream)
                    .withType(type);
            return builder.build();
        } catch (IOException e) {
            log.error("Could not attach to mail, error", e);
            return null;
        }
    }

    private Mail safeAddAttachments(Mail mail, Collection<Attachments> attachments) {
        attachments.stream().filter(Objects::nonNull).forEach(mail::addAttachments);
        return mail;
    }

    private Attachments addInlineImage(InlineImage image) {
        Attachments attachments = new Attachments.Builder("image", image.content)
                .withDisposition("inline").withContentId(image.cid).withType(image.contentType).build();
        log.info("added an image, cid : {}", image.cid);
        return attachments;
    }

    private String searchForImagesInHtml(String htmlContent, List<InlineImage> images) {
        final Matcher matcher = imgRegExp.matcher(htmlContent);
        int i = 0;
        while (matcher.find()) {
            String srcBlock  = matcher.group();
            if (htmlContent.contains(srcBlock)) {
                int startOfSrcToken = srcBlock.indexOf(srcToken);
                int endOfSrcBlock= srcBlock.indexOf( "\"", startOfSrcToken + srcToken.length());
                String dataBlock = srcBlock.substring(startOfSrcToken + srcToken.length(), endOfSrcBlock);
                String contentType = dataBlock.substring("data:".length(), dataBlock.indexOf(";"));
                log.info("contentType = {}", contentType);
                String base64ImageText = dataBlock.split(",")[1];
                log.debug("base64 image text = {}", base64ImageText);
                String cidString = srcBlock.replace(dataBlock, "cid:image" + i);
                htmlContent = htmlContent.replace(srcBlock, cidString);
                images.add(new InlineImage("image"+i, contentType, base64ImageText));
                i++;
            }
        }
        log.debug("html content now looks like: {}", htmlContent);
        return htmlContent;
    }

    // small helper for the above, might exist somewhere in Spring but can't find at present
    @AllArgsConstructor @ToString
    private class InlineImage {
        String cid;
        String contentType;
        String content;
    }

}
