package com.example.authservice.controller;

import com.example.authservice.dto.EmailRequestDTO;
import com.example.authservice.mapper.UserMapper;
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
    private final UserMapper userMapper;

    // 1) 프론트에서 생성한 코드 저장 (POST)
    @PostMapping("/{email:.+}/authcode")
    public ResponseEntity<Void> storeCode(
            @PathVariable String email,
            @RequestBody EmailRequestDTO dto
    ) {
        if (userMapper.countByEmail(email) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        emailService.storeCode(email, dto.getCode());
        return ResponseEntity.ok().build();
    }

    // 2) 코드 검증 → memberId 반환
    // 이메일 인증이 성공했을 때 반환되는 고유 토큰
    @PostMapping("/{email:.+}/authcode/verify")
    public ResponseEntity<String> verify(
            @PathVariable String email,
            @RequestBody EmailRequestDTO dto
    ) throws NoSuchAlgorithmException {
        if (emailService.verifyEmailCode(email, dto.getCode())) {
            return ResponseEntity.ok(emailService.makeMemberId(email));
        }
        return ResponseEntity.notFound().build();
    }
}