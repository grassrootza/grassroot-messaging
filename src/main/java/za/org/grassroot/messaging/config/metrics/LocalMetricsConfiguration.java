package za.org.grassroot.messaging.config.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableMetrics
@Profile({ "localpg" })
@ConditionalOnProperty(name = "grassroot.metrics.enabled", havingValue = "true",  matchIfMissing = false)
public class LocalMetricsConfiguration extends MetricsConfigurerAdapter {

    @Override
    public void configureReporters(MetricRegistry metricRegistry) {
        // registerReporter allows the MetricsConfigurerAdapter to
        // shut down the reporter when the Spring context is closed
        registerReporter(ConsoleReporter
                .forRegistry(metricRegistry)
                .build())
                .start(1, TimeUnit.MINUTES);
    }

}