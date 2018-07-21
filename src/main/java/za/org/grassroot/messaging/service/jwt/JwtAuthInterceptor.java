package za.org.grassroot.messaging.service.jwt;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by luke on 2017/05/22.
 */
@Component @Slf4j
public class JwtAuthInterceptor extends HandlerInterceptorAdapter {

    private static final String BEARER_IDENTIFIER = "Bearer "; // space is important

    private JwtService jwtService;

    @Autowired
    public void setJwtService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("Inside JWT interceptor, checking request ...");
        String authorization = request.getHeader("Authorization");
        log.debug("Header: {}", authorization);
        if (StringUtils.isEmpty(authorization) || !authorization.startsWith(BEARER_IDENTIFIER)) {
            log.error("Error, request with no authorization header, full header: {}",
                    request.getHeaderNames());
            throw new UnauthorizedRequestException("Error! No authorization in the request");
        }

        String jwt = authorization.substring(BEARER_IDENTIFIER.length());

        log.debug("jwtService null? : {}", jwtService == null);

        // will throw JWT error if this is not valid, so we just return true if it passes
        try {
            Jwts.parser()
                    .setSigningKeyResolver(jwtService.getSigningKeyResolver())
                    .parseClaimsJws(jwt);
            return true;
        } catch (Exception e) {
            log.error("JWT error!", e);
            return false;
        }

    }

}
