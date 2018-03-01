package za.org.grassroot.messaging.service.email;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.dto.GrassrootEmail;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

    private final JavaMailSender javaMailSender;
    private final NotificationBroker notificationBroker;

    public EmailSendingBrokerImpl(JavaMailSender javaMailSender, NotificationBroker notificationBroker) {
        this.javaMailSender = javaMailSender;
        this.notificationBroker = notificationBroker;
    }

    @Override
    public void sendNotificationByEmail(Message message) {
        log.info("sending a notification by email ...");
        Notification notification = (Notification) message.getPayload();
        // since email sending may take time, especially if there's a queue, mark it as sending until we know it failed
        notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENDING, null, true, false, null,
                MessagingProvider.EMAIL);

        try {
            MimeMessage mail = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, false); // todo : we may attach calendar invite
            helper.setFrom(fromAddress, "Grassroot");
            helper.setSubject("Grassroot notification");
            helper.setText(notification.getMessage());
            helper.setTo(notification.getTarget().getEmailAddress());
            javaMailSender.send(mail);
            mail.saveChanges();
            // note: docs state Gmail can set header in way that makes getMessageId return wrong value, so recommended is following way
            String messageId = mail.getHeader("Message-ID", "");
            mail.setText("Mail is sent! message Id = {}", messageId);
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.SENT, null, true, false, messageId,
                    MessagingProvider.EMAIL);
        } catch (MessagingException|UnsupportedEncodingException|MailException e) {
            // todo : better handle / distinguish failed sends (and how to check for undeliverable exceptions)
            log.error("Error sending a notification mail1", e);
            notificationBroker.updateNotificationStatus(notification.getUid(), NotificationStatus.DELIVERY_FAILED,
                    "Error sending message via email", true, false, null, MessagingProvider.EMAIL);
        }

    }

    @Async
    @Override
    public void sendNonNotificationEmails(Set<GrassrootEmail> emails) {
        // todo: consider handling queues in here
        log.info("trying to send out a queue of {} emails", emails.size());
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
            
            helper.setReplyTo(email.getFromAddress(), email.getFrom());
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
        log.info("html content now looks like: {}", htmlContent);
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
