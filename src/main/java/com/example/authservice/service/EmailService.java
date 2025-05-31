package com.example.authservice.service;

import com.example.authservice.config.redis.RedisUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class EmailService {
    private final RedisUtil redisUtil;

    // 1) Front에서 받은 코드를 저장
    public void storeCode(String email, String code) {
        System.out.println("✅ storeCode() 호출됨 - email: " + email + ", code: " + code);
        redisUtil.setDataExpire(email, code, 60 * 30L);
    }

    // 2) 검증 로직만 남김
    public boolean verifyEmailCode(String email, String code) {
        String stored = redisUtil.getData(email);
        if (stored == null || !stored.equals(code)) return false;
        redisUtil.setDataExpire(email + ":verified", "true", 600L);
        redisUtil.deleteData(email);
        return true;
    }

    // 검증 성공 시 memberId 생성
    public String makeMemberId(String email) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(email.getBytes());
        md.update(LocalDateTime.now().toString().getBytes());
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
