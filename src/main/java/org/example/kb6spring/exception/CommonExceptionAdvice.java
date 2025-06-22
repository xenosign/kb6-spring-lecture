package org.example.kb6spring.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.exception.user.LoginException;
import org.example.kb6spring.exception.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;

@ControllerAdvice
@Slf4j
public class CommonExceptionAdvice {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> notFoundTodo(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle404(NoHandlerFoundException e) {
        return "/exception/404";
    }

    // 로그인 관련 예외 처리
    @ExceptionHandler(LoginException.class)
    public String handleUserNotFound(LoginException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "/user/login-failed";
    }

    @ExceptionHandler(Exception.class)
    public String exception(Exception e, Model model) {
        log.error("========> 500 에러, {}", e.getMessage());
        e.printStackTrace();

        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("stackTrace", Arrays.asList(e.getStackTrace()));

        return "/exception/500";
    }
}
