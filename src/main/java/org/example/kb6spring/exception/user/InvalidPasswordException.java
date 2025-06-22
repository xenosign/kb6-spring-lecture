package org.example.kb6spring.exception.user;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidPasswordException extends LoginException {
    public InvalidPasswordException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}