package za.org.grassroot.messaging.controller;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import za.org.grassroot.messaging.controller.model.JwtResponse;
import za.org.grassroot.messaging.service.jwt.JwtService;
import za.org.grassroot.messaging.service.jwt.UnauthorizedRequestException;

import java.security.SignatureException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Created by luke on 2017/05/22.
 */
public class BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    protected final JwtService jwtService;

    protected BaseController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    protected String createJwt(Map<String, Object> claims) {
        Objects.requireNonNull(claims);

        Instant now = Instant.now();
        Instant expiry = now.plus(1L, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setHeaderParam("kid", jwtService.getPublicCredentials().getKuid())
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(SignatureAlgorithm.RS256, jwtService.getPrivateKey())
                .compact();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            SignatureException.class, IllegalArgumentException.class, JwtException.class, MalformedJwtException.class
    })
    public JwtResponse badRequest(Exception e) {
        logger.info("Bad request! {}", e.getMessage());
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedRequestException.class)
    public JwtResponse unauthorizedRequest(Exception e) {
        logger.info("Unauthorized! {}", e.getMessage());
        return handleException(e);
    }

    private JwtResponse handleException(Exception e) {
        return new JwtResponse(e.getClass().getName(), e.getMessage());
    }

}
