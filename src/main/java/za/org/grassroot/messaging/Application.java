package za.org.grassroot.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by luke on 2017/05/17.
 */
@SpringBootApplication
@EnableAsync
@EnableJpaRepositories(basePackages = "za.org.grassroot")
@ComponentScan(basePackages = "za.org.grassroot")
@EntityScan(basePackages = "za.org.grassroot", basePackageClasses = Jsr310JpaConverters.class)
public class Application extends SpringApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

}