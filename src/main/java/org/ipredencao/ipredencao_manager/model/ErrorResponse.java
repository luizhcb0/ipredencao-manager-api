package org.ipredencao.ipredencao_manager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer status;
    private String error;
    private String message;
    private String timestamp;

    public ErrorResponse() {}

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(String message) {
        this.error = "Error";
        this.message = message;
    }

    public static ErrorResponse of(HttpStatus httpStatus, String message) {
        ErrorResponse response = new ErrorResponse();
        response.status = httpStatus.value();
        response.error = httpStatus.getReasonPhrase();
        response.message = message;
        response.timestamp = Instant.now().toString();
        return response;
    }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
