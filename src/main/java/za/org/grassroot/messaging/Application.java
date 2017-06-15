package za.org.grassroot.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by luke on 2017/05/17.
 */
@SpringBootApplication
@EnableAsync
@EntityScan(
        basePackageClasses = {Application.class, Jsr310JpaConverters.class}
)
public class Application extends SpringApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

}