package com.example.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleUsernameNotFound(UsernameNotFoundException ex) {
        return "로그인 실패: " + ex.getMessage();
    }

    // 기존 모든 예외 → 500 처리하고 있다면 여기도 유지
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAll(Exception ex) {
        return "에러 발생" + ex.getMessage();
    }

    //이메일 미인증시 404처리
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<?> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("message", ex.getMessage()));
    }

    //이메일 중복시 400처리
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String,String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(Collections.singletonMap("message", ex.getMessage()));
    }

}

