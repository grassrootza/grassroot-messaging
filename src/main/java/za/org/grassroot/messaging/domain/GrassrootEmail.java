package za.org.grassroot.messaging.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Objects;

@Builder @Getter @Setter
public class GrassrootEmail {

    private String fromName;
    private String fromAddress;
    private String toAddress;
    private String subject;
    private String content;
    private String plainTextContent;

    private File attachment;
    private String attachmentName;

    public boolean hasAttachment() {
        return attachment != null;
    }

    public boolean hasPlainText() {
        return !StringUtils.isEmpty(plainTextContent);
    }

    @Override
    public String toString() {
        return "GrassrootEmail{" +
                "fromName='" + fromName + '\'' +
                ", address='" + toAddress + '\'' +
                ", subject='" + subject + '\'' +
//                ", content='" + content + '\'' +
                ", attachmentName='" + attachmentName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrassrootEmail that = (GrassrootEmail) o;
        return Objects.equals(toAddress, that.toAddress) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {

        return Objects.hash(toAddress, subject, content);
    }
}
