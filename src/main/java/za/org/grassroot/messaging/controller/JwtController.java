package za.org.grassroot.messaging.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import za.org.grassroot.messaging.controller.model.JwtResponse;
import za.org.grassroot.messaging.service.jwt.JwtService;
import za.org.grassroot.messaging.service.jwt.PublicCredentials;

import java.time.Instant;
import java.util.Date;

/**
 * Created by luke on 2017/05/22.
 */
@RestController @Slf4j
@RequestMapping("/jwt")
public class JwtController extends BaseController {

    @Autowired
    protected JwtController(JwtService jwtService) {
        super(jwtService);
    }

    @RequestMapping("/public/get")
    public @ResponseBody PublicCredentials getPublicCredentials() {
        return jwtService.getPublicCredentials();
    }

    @RequestMapping("/public/refresh/trusted")
    public @ResponseBody boolean refreshPublicCredentials() {
        log.info("received call to refresh trusted keys");
        return jwtService.refreshTrustedKeys();
    }

    @RequestMapping("/test/parse")
    public JwtResponse testToken(@RequestParam String jwt) {
        return new JwtResponse(Jwts.parser()
                .setSigningKeyResolver(jwtService.getSigningKeyResolver())
                .parseClaimsJws(jwt));
    }

    @RequestMapping("/test/build")
    public JwtResponse testBuild() {
        return new JwtResponse(Jwts.builder()
                .setHeaderParam("kid", jwtService.getPublicCredentials().getKuid())
                .setIssuer("Grassroot")
                .setSubject("testing push")
                .claim("name", "Grassroot Messaging")
                .claim("hasBeenSent", true)
                .setIssuedAt(Date.from(Instant.ofEpochSecond(1495463596L)))   // Mon May 22 2017 16:33:42 GMT+0200 (SAST)
                .setExpiration(Date.from(Instant.ofEpochSecond(4651137196L))) // Mon May 22 2117 16:33:42 GMT+0200 (SAST)
                .signWith(
                        SignatureAlgorithm.RS256,
                        jwtService.getPrivateKey()
                )
                .compact());
    }

}
