package za.org.grassroot.messaging.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;
import za.org.grassroot.core.domain.media.MediaFileRecord;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service @Slf4j
public class StorageBrokerImpl implements StorageBroker {

    private static final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.EU_WEST_1)
            .withCredentials(new ProfileCredentialsProvider("s3images"));

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        s3Client = builder.build();
    }

    @Override
    public File fetchFileFromRecord(MediaFileRecord record) {
        try {
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(record.getBucket(), record.getKey()));
            S3ObjectInputStream s3is = s3Object.getObjectContent();
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

            String extension = "";
            try {
                MimeType type = allTypes.forName(record.getMimeType());
                extension = type.getExtension();
            } catch (NullPointerException|MimeTypeException e) {
                log.error("error! could not work out mime type");
            }

            File outputFile = File.createTempFile(record.getKey(), extension);
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
            return outputFile;
        } catch (SdkClientException e) {
            log.error("Error fetching from S3", e);
            return null;
        } catch (IOException e) {
            log.error("Error handling file", e);
            return null;
        }
    }
}
