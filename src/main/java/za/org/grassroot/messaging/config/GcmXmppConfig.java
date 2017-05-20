package za.org.grassroot.messaging.config;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.xmpp.config.XmppConnectionFactoryBean;
import org.springframework.integration.xmpp.outbound.ChatMessageSendingMessageHandler;
import org.springframework.messaging.MessageChannel;
import za.org.grassroot.messaging.service.gcm.GcmXmppInboundListener;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by luke on 2017/05/19.
 */
@Configuration
@PropertySource(value = "file:${user.home}/grassroot/grassroot-integration.properties", ignoreResourceNotFound = true)
public class GcmXmppConfig {

    private static final Logger logger = LoggerFactory.getLogger(GcmXmppConfig.class);

    @Value("${gcm.connection.url}") private String host;
    @Value("${gcm.connection.port}") private int port;
    @Value("${gcm.sender.id}") private String gcmSenderId;
    @Value("${gcm.sender.key}") private String gcmSenderKey;

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
                .build();
    }

    /*
    @Bean
    public GcmXmppInboundListener inboundAdapter(XMPPConnection connection, MessageChannel gcmInboundChannel) {
        GcmMessageListeningEndpoint endpoint = new GcmMessageListeningEndpoint(connection);
        endpoint.setOutputChannel(gcmInboundChannel);
        endpoint.setAutoStartup(true);
        return endpoint;
    }*/

}
