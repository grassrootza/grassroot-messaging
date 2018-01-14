package za.org.grassroot.messaging.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
public class SendTimeUtil {

    public static Instant restrictSendTime(ZoneId zoneId) {
        ZonedDateTime zdtNow = ZonedDateTime.now(zoneId);
        if (zdtNow.getHour() > 20) {
            return zdtNow.plusDays(1).withHour(7).withMinute(0).toInstant();
        } else if (zdtNow.getHour() < 7){
            return zdtNow.withHour(7).withMinute(0).toInstant();
        } else {
            return zdtNow.toInstant();
        }
    }

}
