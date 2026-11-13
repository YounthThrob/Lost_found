package com.lostfound.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CampusVerificationTest {

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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void send_returns_expiry() throws Exception {
        String token = loginToken();

        mockMvc.perform(post("/api/v1/me/campus-verification/email").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@school.edu.cn\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.expiresInSeconds").value(300));
    }

    @Test
    void send_invalid_email_returns_validation_failed() throws Exception {
        String token = loginToken();

        mockMvc.perform(post("/api/v1/me/campus-verification/email").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void confirm_wrong_code_returns_verification_code_invalid() throws Exception {
        String token = loginToken();

        mockMvc.perform(post("/api/v1/me/campus-verification/email/confirm").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@school.edu.cn\",\"code\":\"000000\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VERIFICATION_CODE_INVALID"));
    }

    @Test
    void confirm_success_sets_email_verified() throws Exception {
        String token = loginToken();
        String header = bearer(token);

        mockMvc.perform(post("/api/v1/me/campus-verification/email/confirm").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@school.edu.cn\",\"code\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("EMAIL_VERIFIED"))
            .andExpect(jsonPath("$.data.verifiedEmail").value("student@school.edu.cn"))
            .andExpect(jsonPath("$.data.emailVerifiedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("EMAIL_VERIFIED"));
    }

    @Test
    void reconfirm_overwrites_verified_email() throws Exception {
        String token = loginToken();
        String header = bearer(token);

        mockMvc.perform(post("/api/v1/me/campus-verification/email/confirm").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@school.edu.cn\",\"code\":\"123456\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/me/campus-verification/email/confirm").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@school.edu.cn\",\"code\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("EMAIL_VERIFIED"))
            .andExpect(jsonPath("$.data.verifiedEmail").value("new@school.edu.cn"));
    }
}
