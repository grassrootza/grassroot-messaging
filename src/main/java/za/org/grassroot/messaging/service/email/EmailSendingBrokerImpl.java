package za.org.grassroot.messaging.service.email;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.util.Duration;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.domain.User;
import za.org.grassroot.core.domain.media.MediaFileRecord;
import za.org.grassroot.core.domain.notification.EventNotification;
import za.org.grassroot.core.domain.task.Event;
import za.org.grassroot.core.dto.GrassrootEmail;
import za.org.grassroot.core.enums.EventType;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.core.repository.MediaFileRecordRepository;
import za.org.grassroot.messaging.service.NotificationBroker;
import za.org.grassroot.messaging.service.StorageBroker;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service @Slf4j
@ConditionalOnProperty(value = "grassroot.email.enabled", havingValue = "true")
public class EmailSendingBrokerImpl implements EmailSendingBroker {

    private static final Pattern imgRegExp  = Pattern.compile( "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>" );
    private static final String srcToken = "src=\"";

    @Value("${grassroot.notifications.email.from:notifications@grassroot.org.za}")
    private String fromAddress;

    @Value("${grassroot.email.default.name:Grassroot}")
    private String defaultFromName;

    private static final String DEFAULT_SUBJECT = "Grassroot notification";
    private static final String NOTIFICATION_BODY_HTML = "<p>Dear %1$s</p><p><b>Notice: </b>%2$s</p><p>%3$s</p>";

    private static final String NOTIFICATION_BODY_TEXT = "Dear %1$s,\n\n%2$s\n\n%3$s";
    private static final String NOTIFICATION_FOOTER_PLAIN = "Sent by Grassroot";
    private static final String NOTIFICATION_FOOTER_ACCOUNT = "Sent for %1$s by Grassoot";

    private final JavaMailSender javaMailSender;
    private final NotificationBroker notificationBroker;
    private final MediaFileRecordRepository recordRepository;
    private final StorageBroker storageBroker;

    public EmailSendingBrokerImpl(JavaMailSender javaMailSender, NotificationBroker notificationBroker, MediaFileRecordRepository recordRepository, StorageBroker storageBroker) {
        this.javaMailSender = javaMailSender;
        this.notificationBroker = notificationBroker;
        this.recordRepository = recordRepository;
        this.storageBroker = storageBroker;
    }

    @Override
    @Transactional
    public void sendNotificationByEmail(Message message) {
//        DebugUtil.transactionRequired("");
        log.info("sending a notification by email ...");
        Notification notification = (Notification) message.getPayload();
        // since email sending may take time, especially if there's a queue, mark it as sending until we know it failed
        notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENDING, null, true, false, null,
                MessagingProvider.EMAIL);
        log.info("status updated, proceeding with: {}", notification);

        try {
            MimeMessage mail = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);

            User sender = notification.getSender();
            if (sender != null && sender.hasEmailAddress()) {
                helper.setFrom(sender.getEmailAddress(), sender.getName());
                helper.setReplyTo(sender.getEmailAddress(), sender.getName());
            } else {
                helper.setFrom(fromAddress, defaultFromName);
            }

            if (notification instanceof EventNotification) {
                Event event = ((EventNotification) notification).getEvent();
                helper.setSubject(event.getAncestorGroup().getName() + ": " + event.getName());
            } else {
                helper.setSubject(DEFAULT_SUBJECT);
            }
            // whole templating language for this is going to be too much, so just using basic strings
            User target = notification.getTarget();
            String footer = sender == null || sender.getPrimaryAccount() == null ? NOTIFICATION_FOOTER_PLAIN :
                    String.format(NOTIFICATION_FOOTER_ACCOUNT, sender.getName());
            helper.setText(String.format(NOTIFICATION_BODY_TEXT, target.getName(), notification.getMessage(), footer),
                    String.format(NOTIFICATION_BODY_HTML, target.getName(), notification.getMessage(), footer));
            helper.setTo(notification.getTarget().getEmailAddress());

            ByteArrayResource calendarAttachment = notification instanceof EventNotification ?
                    calendarAttachment((EventNotification) notification) : null;
            log.debug("do we have a calendar attachment? : {}", calendarAttachment);
            if (calendarAttachment != null) {
                log.debug("attaching calendar invite ...");
                helper.addAttachment("meeting.ics", calendarAttachment);
            }

            javaMailSender.send(mail);
            mail.saveChanges();
            // note: docs state Gmail can set header in way that makes getMessageId return wrong value, so recommended is following way
            String messageId = mail.getHeader("Message-ID", "");
            mail.setText("Mail is sent! message Id = {}", messageId);
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENT, null, true, false, messageId,
                    MessagingProvider.EMAIL);
        } catch (MessagingException|UnsupportedEncodingException|MailException e) {
            // todo : better handle / distinguish failed sends (and how to check for undeliverable exceptions)
            log.error("Error sending a notification mail", e);
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED,
                    "Error sending message via email", true, false, null, MessagingProvider.EMAIL);
        } catch (Exception e) {
            log.error("Unknown exception: ", e);
        }
    }

    private ByteArrayResource calendarAttachment(EventNotification notification) {
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

        String iCalendarAsString = Biweekly.write(ical).go();
        try {
            return new ByteArrayResource(iCalendarAsString.getBytes(StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            log.error("Something went wrong writing to iCal", e);
            return null;
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
                        javaMailSender.send(mail);
                    } catch (MailException e) {
                        log.error("Error inside java mail sending loop", e);
                    }
                });
        log.info("Sent emails!");
    }

    private MimeMessage transformEmail(GrassrootEmail email) {
        try {
            MimeMessage javaMail = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(javaMail, true);
            
            helper.setTo(email.getAddress());
            helper.setSubject(email.getSubject());
            
            boolean hasFromAddress = !StringUtils.isEmpty(email.getFromAddress());
            boolean hasFrom = !StringUtils.isEmpty(email.getFrom());
            
            helper.setFrom(hasFromAddress ? email.getFromAddress() : fromAddress, hasFrom ? email.getFrom() : defaultFromName);
            if (hasFromAddress && hasFrom) {
                helper.setReplyTo(email.getFromAddress(), email.getFrom());
            } else if (hasFromAddress) {
                helper.setReplyTo(email.getFrom());
            }

            // note: we assume default is html content
            if (email.hasHtmlContent()) {
                List<InlineImage> imgMap = new ArrayList<>();
                String htmlContent = email.getHtmlContent();
                htmlContent = searchForImagesInHtml(htmlContent, imgMap);
                log.info("traversed, image map = {}", imgMap);
                log.debug("added images, done, html content = {}", htmlContent);
                // also just to remove the image, in case it's there
                final String text = searchForImagesInHtml(email.getContent(), new ArrayList<>());
                helper.setText(text, htmlContent);
                imgMap.forEach(img -> addInlineImage(helper, img));
            } else {
                helper.setText(email.getContent());
            }

            if (email.hasAttachment()) {
                helper.addAttachment(email.getAttachmentName(), email.getAttachment());
            }

            log.info("email attachments and UIDs = {}", email.getAttachmentUidsAndNames());

            if (email.getAttachmentUidsAndNames() != null) {
                email.getAttachmentUidsAndNames().forEach((uid, name) -> {
                    MediaFileRecord record = recordRepository.findOneByUid(uid);
                    File fileToAttach = storageBroker.fetchFileFromRecord(record);
                    final String attachmentName = !StringUtils.isEmpty(name) ? name :
                            !StringUtils.isEmpty(record.getFileName()) ? record.getFileName() : fileToAttach.getName();
                    try {
                        helper.addAttachment(attachmentName, fileToAttach);
                    } catch(MessagingException e) {
                        log.error("could not attach! name: ", name);
                    }
                });
            }

            return javaMail;
        } catch (MessagingException|UnsupportedEncodingException|MailException e) {
            log.error("Error transforming mail! Email: {}, exception: {}", email, e);
            return null;
        }
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
                ByteArrayResource convertedStream = new ByteArrayResource(Base64Utils.decodeFromString(base64ImageText));
                String cidString = srcBlock.replace(dataBlock, "cid:image" + i);
                htmlContent = htmlContent.replace(srcBlock, cidString);
                images.add(new InlineImage("image"+i, contentType, convertedStream));
                i++;
            }
        }
        log.debug("html content now looks like: {}", htmlContent);
        return htmlContent;
    }

    private void addInlineImage(MimeMessageHelper helper, InlineImage image) {
        try {
            helper.addInline(image.cid, image.inputStream, image.contentType);
            log.info("added an image, cid : {}", image.cid);
        } catch (MessagingException e) {
            log.error("error adding message with id {}, error {}", image.cid, e);
        }
    }

    // small helper for the above, might exist somewhere in Spring but can't find at present
    @AllArgsConstructor @ToString
    private class InlineImage {
        String cid;
        String contentType;
        ByteArrayResource inputStream;
    }

}
