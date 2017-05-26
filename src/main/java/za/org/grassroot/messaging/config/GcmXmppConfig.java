package za.org.grassroot.messaging.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.xmpp.config.XmppConnectionFactoryBean;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by luke on 2017/05/19.
 */
@Configuration
@PropertySource(value = "file:${grassroot.messaging.properties.path}", ignoreResourceNotFound = true)
public class GcmXmppConfig {

    private static final Logger logger = LoggerFactory.getLogger(GcmXmppConfig.class);

    @Value("${gcm.connection.url}") private String host;
    @Value("${gcm.connection.port}") private int port;
    @Value("${gcm.sender.id}") private String gcmSenderId;
    @Value("${gcm.sender.key}") private String gcmSenderKey;

    @Value("${grassroot.gcm.debugging.enabled:false}") private boolean gcmDebuggingEnabled;

    @Bean
    public XmppConnectionFactoryBean xmppConnectionFactoryBean() {
        logger.info("Starting up XMPP connection, for URL={} on port={}, with ID={} and key={}", host, port, gcmSenderId, gcmSenderKey);
        XmppConnectionFactoryBean connectionFactoryBean = new XmppConnectionFactoryBean();
        connectionFactoryBean.setConnectionConfiguration(connectionConfiguration());
        connectionFactoryBean.setAutoStartup(true);
        Roster.setRosterLoadedAtLoginDefault(false);
        logger.info("XMPP connection successfully started up");
        return connectionFactoryBean;
    }

    @Primary
    @Bean("gcmObjectMapper")
    public ObjectMapper gcmObjectMapper() {
        ObjectMapper payloadMapper = new ObjectMapper();
        payloadMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        payloadMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        payloadMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        return payloadMapper;
    }

    private XMPPTCPConnectionConfiguration connectionConfiguration() {
        return XMPPTCPConnectionConfiguration
                .builder()
                .setServiceName(host)
                .setCompressionEnabled(true)
                .setHost(host)
                .setPort(port)
                .setUsernameAndPassword(gcmSenderId, gcmSenderKey)
                .setSocketFactory(SSLSocketFactory.getDefault())
                .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                .setDebuggerEnabled(gcmDebuggingEnabled)
                .build();
    }

}
