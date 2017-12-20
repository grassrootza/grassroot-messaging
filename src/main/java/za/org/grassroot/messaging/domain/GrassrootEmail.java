package za.org.grassroot.messaging.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.io.File;

@Builder @Getter @Setter
public class GrassrootEmail {

    private String fromName;
    private String fromAddress;
    private String toAddress;
    private String subject;
    private String content;
    private String htmlContent;

    private File attachment;
    private String attachmentName;

    public boolean hasAttachment() {
        return attachment != null;
    }

    public boolean hasHtmlContent() {
        return !StringUtils.isEmpty(htmlContent);
    }

    @Override
    public String toString() {
        return "GrassrootEmail{" +
                "fromName='" + fromName + '\'' +
                ", address='" + toAddress + '\'' +
                ", subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", attachmentName='" + attachmentName + '\'' +
                '}';
    }
}
