package za.org.grassroot.messaging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.client.RestTemplate;
import za.org.grassroot.messaging.config.HttpConfig;

import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by luke on 2016/05/17.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { HttpConfig.class })
public class HttpClientTest {

    private static final String url = "http://httpbin.org/status/200";

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void sync() {
        ResponseEntity<Map> entity = restTemplate.getForEntity(url, Map.class);
        assertTrue(entity.getStatusCode().equals(HttpStatus.OK));
    }

}