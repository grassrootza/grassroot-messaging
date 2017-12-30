package za.org.grassroot.messaging.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import za.org.grassroot.messaging.controller.model.EmailResponse;
import za.org.grassroot.messaging.domain.GrassrootEmail;
import za.org.grassroot.messaging.service.email.EmailSendingBroker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
NOTE: convert to Kafka as soon as possible, basically
 */
@RestController @Slf4j
@RequestMapping("/email")
public class EmailController {

    private final EmailSendingBroker emailSendingBroker;

    public EmailController(EmailSendingBroker emailSendingBroker) {
        this.emailSendingBroker = emailSendingBroker;
    }

    @RequestMapping(value = "/send")
    public ResponseEntity<EmailResponse> asyncEmailSend(@RequestParam(required = false) String fromName,
                                                        @RequestParam(required = false) String fromAddress,
                                                        @RequestParam(required = false) String attachmentName,
                                                        @RequestParam List<String> addresses,
                                                        @RequestParam String subject,
                                                        @RequestParam(required = false) String textContent,
                                                        @RequestParam(required = false) String content,
                                                        @RequestBody(required = false) MultipartFile attachment) {
        GrassrootEmail.GrassrootEmailBuilder baseBuilder = GrassrootEmail.builder();

        log.info("do we have an attachment? {}", attachment != null);

        // email builder will use defaults if any of these are null
        baseBuilder.subject(subject)
                .fromName(fromName)
                .fromAddress(fromAddress)
                .plainTextContent(textContent)
                .content(content);

        File toAttach = convertAttachment(attachment);
        log.info("to attach, came back okay? {}", toAttach !=null);
        if (attachment != null) {
            baseBuilder.attachmentName(attachmentName);
            baseBuilder.attachment(toAttach);
        }

        Set<GrassrootEmail> emails = addresses.stream()
                .map(a -> baseBuilder.toAddress(a).build())
                .collect(Collectors.toSet());

        log.info("received list of {} addresses, generated {} emails, exiting", addresses.size(), emails.size());
        emailSendingBroker.sendNonNotificationEmails(emails);

        return ResponseEntity.ok(EmailResponse.EMAILS_QUEUED);
    }

    private File convertAttachment(MultipartFile file) {
        if (file == null) {
            return null;
        }

        try {
            log.info("trying to create attachment");
            File tempStore = File.createTempFile("attachment", "temp");
            tempStore.createNewFile();
            tempStore.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempStore);
            fos.write(file.getBytes());
            fos.close();
            return tempStore;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
