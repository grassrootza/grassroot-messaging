package za.org.grassroot.messaging.scheduling.receipts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.org.grassroot.core.repository.UserRepository;
import za.org.grassroot.messaging.service.email.BouncedEmailResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service @Slf4j
@ConditionalOnProperty(value = "grassroot.email.enabled", havingValue = "true")
public class FailedEmailResponseFetcherImpl implements FailedEmailResponseFetcher {

    @Value("${SENDGRID_API_KEY:testing}")
    private String sendGridApiKey;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FailedEmailResponseFetcherImpl(UserRepository userRepository,
                                          @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void fetchInvalidEmailAddesses() {
        try {
            log.info("fetching invalid email addresses ... API key = {}", sendGridApiKey);
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setEndpoint("suppression/bounces");
            request.setMethod(Method.GET);
            request.addQueryParam("start_time", "" + LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond());
            request.addQueryParam("end_time", "" + Instant.now().getEpochSecond());
            request.addQueryParam("limit", "10"); // to not overload DB
            Response response = sg.api(request);
            log.info("body of response: {}, status: {}, headers: {}", response.getBody(), response.getStatusCode(), response.getHeaders());
            BouncedEmailResponse[] bounceResponses = objectMapper.readValue(response.getBody(), BouncedEmailResponse[].class);
            if (bounceResponses != null && bounceResponses.length > 0) {
                List<String> bouncedAddresses = Arrays.stream(bounceResponses).map(BouncedEmailResponse::getEmail).collect(Collectors.toList());
                bouncedAddresses.stream().map(userRepository::findByEmailAddressAndEmailAddressNotNull)
                        .filter(Objects::nonNull).forEach(user -> user.setContactError(true));
                clearFailedEmails(bouncedAddresses);
            }
        } catch (IOException e) {
            log.error("Error fetching invalid emails! Error: {}", e);
        }
    }

    private void clearFailedEmails(List<String> failedEmails) {
        try {
            log.info("clearing failed emails: {}", failedEmails);
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.DELETE);
            request.setEndpoint("suppression/bounces");
            String emailList = failedEmails.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
            request.setBody("{\"emails\":[" + emailList + "]}");
            log.info("delete request body: {}", request.getBody());
            Response response = sg.api(request);
            log.info("completed deletion: status: {}, body: {}, headers: {}", response.getStatusCode(), response.getBody(), response.getHeaders());
        } catch (IOException e) {
            log.error("Could not clear email from log, error: ", e);
        }
    }
}
