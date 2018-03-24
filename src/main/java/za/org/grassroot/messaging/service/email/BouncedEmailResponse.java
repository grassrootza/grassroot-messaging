package za.org.grassroot.messaging.service.email;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString
public class BouncedEmailResponse {

    private long created;
    private String email;
    private String reason;
    private String status;

}
