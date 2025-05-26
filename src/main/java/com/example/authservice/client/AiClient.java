package com.example.authservice.client;

import com.example.authservice.dto.AllergyInfoRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "aiClient", url = "${ai.url}/api/ai")
public interface AiClient {

    @PostMapping("/user-allergy")
    void sendAllergyInfo(@RequestBody AllergyInfoRequestDTO allergyInfoRequestDTO);

    @PutMapping("/user-allergy")
    void updateAllergyInfo(@RequestBody AllergyInfoRequestDTO allergyInfoRequestDTO);

}
