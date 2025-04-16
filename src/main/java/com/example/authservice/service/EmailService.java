package com.example.authservice.service;

import com.example.authservice.config.redis.RedisUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;

    @Value("${spring.mail.username}")
    private String configEmail;

    // 인증 코드 생성
    private String createdCode() {
        int leftLimit = 48; // number '0'
        int rightLimit = 122; // alphabet 'z'
        int targetStringLength = 6;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <=57 || i >=65) && (i <= 90 || i>= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    // 인증 메일 생성
    private String setContext(String code) throws IOException {
        // 1. 템플릿 파일을 클래스패스에서 읽어온다고 가정
        //    (resources/templates/mail.html 위치)
        InputStream inputStream = getClass().getResourceAsStream("/templates/mail.html");
        if (inputStream == null) {
            throw new IllegalStateException("템플릿 파일을 찾을 수 없습니다.");
        }

        // 2. 템플릿 파일 내용을 문자열로 로드
        String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // 3. 플레이스홀더(`{{code}}`)를 실제 인증 코드로 치환
        return template.replace("{{code}}", code);
    }


    // 메일 반환
    private MimeMessage createEmailForm(String email) throws MessagingException, IOException {

        String authCode = createdCode();

        MimeMessage message = mailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, email);
        message.setSubject("안녕하세요 인증번호입니다.");
        message.setFrom(configEmail);
        message.setText(setContext(authCode), "utf-8", "html");

        redisUtil.setDataExpire(email, authCode, 60 * 30L);

        return message;
    }


    // 메일 보내기
    public void sendEmail(String toEmail) throws MessagingException, IOException {
        if (redisUtil.existData(toEmail)) {
            redisUtil.deleteData(toEmail);
        }

        MimeMessage emailForm = createEmailForm(toEmail);

        mailSender.send(emailForm);
    }

    // 코드 검증
    public boolean verifyEmailCode(String email, String code) {
        String codeFoundByEmail = redisUtil.getData(email);
        if (codeFoundByEmail == null) {
            return false;
        }
        redisUtil.setDataExpire(email + ":verified", "true", 600L); // 10분간 유효
        return true;
    }

    public String makeMemberId(String email) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(email.getBytes());
        md.update(LocalDateTime.now().toString().getBytes());
        byte[] digest = md.digest();

        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
