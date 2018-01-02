package za.org.grassroot.messaging;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import za.org.grassroot.core.domain.User;

/**
 * Created by paballo on 2016/04/12.
 */
@RunWith(SpringRunner.class)
public class IncomingGcmHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(IncomingGcmHandlerTest.class);

    private String registration =" {\n" +
            "      \"category\":\"com.techmorphosis.grassroot.gcm\",\n" +
            "      \"data\":\n" +
            "         {\n" +
            "         \"action\":\n" +
            "            \"REGISTER\",\n" +
            "         \"phoneNumber\":\"0616780986\"\n" +
            "         },\n" +
            "      \"message_id\":\"20\",\n" +
            "      \"from\":\"someRegistrationId\"\n" +
            "      }";

    @Test
    public void handleUpstream() throws Exception{
        User user = makeDummy("0616780986", "some name");
        log.info("Constructed user={}", user);
        log.info("Created and saved user={}", user);
      //  gcmService.registerUser(new User("0616780986"),"someRegistrationId");
        Message message = new Message();
        message.addExtension(new GcmPacketExtension(registration));
      //  messageHandler.handleUpstreamMessage(message);
        
    }

    // for tests
    public User makeDummy(String phoneNumber, String displayName) {
        return new User(phoneNumber, displayName, null);
    }



}

