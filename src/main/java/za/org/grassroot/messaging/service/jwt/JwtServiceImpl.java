package za.org.grassroot.messaging.service.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.TextCodec;
import io.jsonwebtoken.impl.crypto.RsaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by luke on 2017/05/22.
 */
@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    @Value("${grassroot.service.key.urls}")
    private String[] authorizedServiceKeyUrls;

    private final RestTemplate restTemplate;

    private KeyPair keyPair;
    private String kuid;
    private Map<String, PublicKey>  publicKeyMap = new HashMap<>();

    @Autowired
    public JwtServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        PublicCredentials credentials = refreshPublicCredentials();
        logger.info("refreshed credentials: {}, now adding authorized list", credentials);
        Arrays.asList(authorizedServiceKeyUrls).forEach(this::getAndAddAuthorizedPublicKey);
    }

    private void getAndAddAuthorizedPublicKey(String url) {
        try {
            ResponseEntity<PublicCredentials> getResponse = restTemplate.getForEntity(url, PublicCredentials.class);
            addTrustedCredentials(getResponse.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting public credentials: {}", e.getMessage());
        }
    }

    public SigningKeyResolver signingKeyResolver = new SigningKeyResolverAdapter() {
        @Override
        public Key resolveSigningKey(JwsHeader header, Claims claims) {
            if (StringUtils.isEmpty(header.getKeyId())) {
                throw new JwtException("Missing required kid header param in JWT with claims: " + claims);
            }
            if (!publicKeyMap.containsKey(header.getKeyId())) {
                throw new JwtException("No public key registered for key ID: " + header.getKeyId());
            }
            return publicKeyMap.get(header.getKeyId());
        }
    };

    @Override
    public PublicCredentials getPublicCredentials() {
        return createCredentialEntity(kuid, keyPair.getPublic());
    }

    @Override
    public PublicCredentials refreshPublicCredentials() {
        keyPair = RsaProvider.generateKeyPair(1024);
        kuid = UUID.randomUUID().toString();
        PublicCredentials publicCredentials = createCredentialEntity(kuid, keyPair.getPublic());
        addTrustedCredentials(publicCredentials); // we trust ourselves ...
        return publicCredentials;
    }

    @Override
    public void addTrustedCredentials(PublicCredentials publicCredentials) {
        byte[] encoded = TextCodec.BASE64URL.decode(publicCredentials.getB64UrlPublicKey());
        PublicKey publicKey = null;
        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
            logger.info("Unable to create public key: {}", e.getMessage());
        }

        publicKeyMap.put(publicCredentials.getKuid(), publicKey);
    }

    @Override
    public SigningKeyResolver getSigningKeyResolver() {
        return signingKeyResolver;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    private PublicCredentials createCredentialEntity(String kuid, PublicKey key) {
        return new PublicCredentials(kuid, TextCodec.BASE64.encode(key.getEncoded()));
    }

}
