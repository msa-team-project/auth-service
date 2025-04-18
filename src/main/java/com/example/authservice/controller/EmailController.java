package com.example.authservice.controller;

import com.example.authservice.dto.EmailRequestDTO;
import com.example.authservice.service.EmailService;
import com.example.authservice.service.UserService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/auths/email")
public class EmailController {

    private final EmailService emailService;
    private final UserService userService;

    // 이메일로 인증 코드 받기
    @GetMapping("/{email}/authcode")
    public ResponseEntity<String> sendEmailPath(@PathVariable String email) throws MessagingException, IOException {
        emailService.sendEmail(email);
        log.info("Requested email: " + email);
        return ResponseEntity.ok("이메일을 확인하세요");
    }

    // 인증 코드를 입력한 후 난수 생성
    @PostMapping("/{email}/authcode")
    public ResponseEntity<String> sendEmailAndCode(@PathVariable String email, @RequestBody EmailRequestDTO emailRequestDTO) throws NoSuchAlgorithmException {
        if (emailService.verifyEmailCode(email, emailRequestDTO.getCode())) {
            return ResponseEntity.ok(emailService.makeMemberId(email));
        }
        return ResponseEntity.notFound().build();
    }
}
