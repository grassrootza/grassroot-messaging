package za.org.grassroot.messaging.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by luke on 2017/05/22.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestResponse<P> {

    protected String message;
    protected Status status;
    protected P data;

    public enum Status {
        SUCCESS, ERROR
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public P getData() {
        return data;
    }

    public void setData(P data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RestResponse{" +
                "message='" + message + '\'' +
                ", status=" + status +
                ", data=" + data +
                '}';
    }
}
