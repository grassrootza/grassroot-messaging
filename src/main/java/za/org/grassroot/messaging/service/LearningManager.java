package za.org.grassroot.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import za.org.grassroot.messaging.domain.exception.SeloParseDateTimeFailure;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ported by Luke on 2017/05/20. Only true duplicate from main platform. Necessary as we have to call this from both.
 */
@Service
public class LearningManager implements LearningService {

    private Logger log = LoggerFactory.getLogger(LearningManager.class);

    private RestTemplate restTemplate;
    private Environment environment;

    private String learningHost;
    private int learningPort;
    private String dateTimePath;
    private String dateTimeParam;

    @Autowired
    public LearningManager(RestTemplate restTemplate, Environment environment) {
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        learningHost = environment.getProperty("grassroot.learning.host", "localhost"); // default to localhost if not set
        learningPort = environment.getProperty("grassroot.learning.port", Integer.class, 9000);
        dateTimePath = environment.getProperty("grassroot.learning.datetime.path", "parse");
        dateTimeParam = environment.getProperty("grassroot.learning.datetime.param", "phrase");
    }

    @Override
    public LocalDateTime parse(String phrase) {
        LocalDateTime parsedDateTime;
        try {
            // note : the rest template is autowired to use a default character encoding (UTF 8), so putting encode
            // here will double encode and throw errors, hence leave it out
            String url = UriComponentsBuilder.newInstance()
                    .scheme("http")
                    .host(learningHost)
                    .port(learningPort)
                    .path(dateTimePath)
                    .queryParam(dateTimeParam, phrase)
                    .build()
                    .toUriString();

            String s = this.restTemplate.getForObject(url, String.class);
            if ("ERROR_PARSING".equals(s)) {
                // throw error so can tell user didn't understand, preferable to returning current date time
                throw new SeloParseDateTimeFailure();
            } else {
                parsedDateTime = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE); // might do via an incoming binder, but would just perform same operation
                return parsedDateTime;
            }
        } catch (Exception e) {
            // throw an error because this shouldn't happen (might be because of an error reaching the server ...)
            log.error("Error calling Selo! Error message: {}", e.toString());
            throw new SeloParseDateTimeFailure();
        }
    }


}
