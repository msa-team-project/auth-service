package com.example.authservice.client;

import com.example.authservice.dto.UpdateProfileRequestDTO;
import com.example.authservice.dto.UserJoinRequestDTO;
import com.example.authservice.grpc.AiServiceGrpc;
import com.example.authservice.grpc.SaveAllergyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGrpcClient {

    @GrpcClient("ai-service")
    private AiServiceGrpc.AiServiceBlockingStub aiStub;

    public void sendAllergyInfo(UserJoinRequestDTO dto, int userUid) {
        if (dto.getAllergies() == null || dto.getAllergies().isEmpty()) {
            log.info("알러지 정보가 없으므로 AI 호출 생략.");
            return;
        }

        SaveAllergyRequest request = SaveAllergyRequest.newBuilder()
                .setUserUid(userUid)
                .addAllAllergies(dto.getAllergies())
                .build();

        try {
            aiStub.sendAllergyInfo(request);
            log.info("AI 알러지 등록 완료");
        } catch (Exception e) {
            log.warn("AI 알러지 등록 실패: {}", e.getMessage(), e);
            throw e; // 필요 시 서비스 측에서 후처리
        }
    }

    public void updateAllergyInfo(UpdateProfileRequestDTO dto, boolean isSocial) {
        if (dto.getAllergies() == null || dto.getAllergies().isEmpty()) {
            log.info("알러지 정보가 없으므로 삭제 요청.");
            return;
        }

        SaveAllergyRequest.Builder builder = SaveAllergyRequest.newBuilder()
                .addAllAllergies(dto.getAllergies());

        if (isSocial) {
            builder.setSocialUid(dto.getUid());
        } else {
            builder.setUserUid(dto.getUid());
        }

        try {
            aiStub.updateAllergyInfo(builder.build());
            log.info("AI 알러지 수정 완료");
        } catch (Exception e) {
            log.warn("AI 알러지 수정 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}
