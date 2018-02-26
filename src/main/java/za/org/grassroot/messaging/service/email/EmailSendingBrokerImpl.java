package za.org.grassroot.messaging.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.org.grassroot.core.domain.Notification;
import za.org.grassroot.core.domain.NotificationStatus;
import za.org.grassroot.core.dto.GrassrootEmail;
import za.org.grassroot.core.enums.MessagingProvider;
import za.org.grassroot.messaging.service.NotificationBroker;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Set;

@Service @Slf4j
@ConditionalOnProperty(value = "grassroot.email.enabled", havingValue = "true")
public class EmailSendingBrokerImpl implements EmailSendingBroker {

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
            helper.setFrom(StringUtils.isEmpty(email.getFromAddress()) ? fromAddress : email.getFromAddress(),
                    StringUtils.isEmpty(email.getFrom()) ? defaultFromName : email.getFrom());
            helper.setSubject(email.getSubject());
            helper.setTo(email.getAddress());
            // note: we assume default is html content
            helper.setText(email.hasHtmlContent() ? email.getHtmlContent() : email.getContent(), email.getContent());
            if (email.hasAttachment()) {
                helper.addAttachment(email.getAttachmentName(), email.getAttachment());
            }
            return javaMail;
        } catch (MessagingException|UnsupportedEncodingException|MailException e) {
            log.error("Error transforming mail! Email: {}, exception: {}", email, e);
            return null;
        }
    }

}
