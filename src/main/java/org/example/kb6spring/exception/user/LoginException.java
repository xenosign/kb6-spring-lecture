package org.example.kb6spring.exception.user;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class LoginException extends RuntimeException {
    private final HttpStatus httpStatus;

    public LoginException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}