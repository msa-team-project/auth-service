package com.example.authservice.client;

import com.example.authservice.dto.AllergyInfoRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "aiClient", url = "${ai.url}/api/ai")
public interface AiClient {

    @PostMapping("users/{userUid}/allergies")
    void sendAllergyInfo(@PathVariable(name = "userUid") Long userUid, @RequestBody AllergyInfoRequestDTO allergyInfoRequestDTO);

}
