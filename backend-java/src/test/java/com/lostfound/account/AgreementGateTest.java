package com.lostfound.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgreementGateTest {

    @TestConfiguration
    static class Config {
        @Bean
        AgreementGateTest.GuardedController guardedController() {
            return new AgreementGateTest.GuardedController();
        }
    }

    @RestController
    static class GuardedController {
        @GetMapping("/test/guarded")
        @RequiresAgreement
        Map<String, Object> guarded() {
            return Map.of("ok", true);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @BeforeEach
    void resetStore() {
        userStore.clear();
    }

    private String loginToken() throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid-code\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    @Test
    void guarded_endpoint_rejects_unconfirmed_user() throws Exception {
        String token = loginToken();

        mockMvc.perform(get("/test/guarded").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("AGREEMENT_NOT_ACCEPTED"));
    }

    @Test
    void guarded_endpoint_allows_confirmed_user() throws Exception {
        String token = loginToken();
        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/test/guarded").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }
}
