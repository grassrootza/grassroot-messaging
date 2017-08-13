package za.org.grassroot.messaging.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableMetrics
@Profile({"production"})
@PropertySource(value = "file:${grassroot.messaging.properties.path}", ignoreResourceNotFound = true)
public class ProductionMetricsConfiguration extends MetricsConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProductionMetricsConfiguration.class);

    @Value("${grassroot.graphite.host:localhost}")
    private String graphiteHost;

    @Value("${grassroot.graphite.port:9000}")
    private int graphitePort;

    @Value("${grassroot.graphite.poll.interval:1000}")
    private long graphiteAmountOfTimeBetweenPolls;

    @Value("${grassroot.graphite.prefix:grassroot}")
    private String graphitePrefix;

    @Override
    public void configureReporters(MetricRegistry metricRegistry) {
        registerReporter(GraphiteReporter
                .forRegistry(metricRegistry)
                .prefixedWith(graphitePrefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(graphite()))
                .start(graphiteAmountOfTimeBetweenPolls, TimeUnit.MILLISECONDS);
    }

    private Graphite graphite() {
        logger.info("starting up graphite, host = {}, port = {}", graphiteHost, graphitePort);
        return new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
    }

}
