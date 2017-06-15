package za.org.grassroot.messaging.service.jwt;

import io.jsonwebtoken.SigningKeyResolver;

import java.security.PrivateKey;

/**
 * Created by luke on 2017/05/22.
 */
public interface JwtService {

    PublicCredentials getPublicCredentials();
    boolean refreshTrustedKeys();
    SigningKeyResolver getSigningKeyResolver();
    PrivateKey getPrivateKey();

}
