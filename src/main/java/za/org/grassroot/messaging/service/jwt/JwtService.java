package za.org.grassroot.messaging.service.jwt;

import io.jsonwebtoken.SigningKeyResolver;

import java.security.PrivateKey;

/**
 * Created by luke on 2017/05/22.
 */
public interface JwtService {

    PublicCredentials getPublicCredentials();
    PublicCredentials refreshPublicCredentials();
    void addTrustedCredentials(PublicCredentials publicCredentials);
    SigningKeyResolver getSigningKeyResolver();
    PrivateKey getPrivateKey();

}
