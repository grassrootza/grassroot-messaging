package za.org.grassroot.messaging.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * Created by luke on 2017/05/22.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtResponse extends RestResponse<Jws<Claims>> {

    private String exceptionType;
    private String jwt;

    public JwtResponse(String jwt) {
        this.jwt = jwt;
        setStatus(Status.SUCCESS);
    }

    public JwtResponse(Jws<Claims> jwsClaims) {
        this.data = jwsClaims;
        setStatus(Status.SUCCESS);
    }

    public JwtResponse(String exceptionType, String message) {
        setStatus(Status.ERROR);
        this.exceptionType = exceptionType;
        this.message = message;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
