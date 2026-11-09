package com.lostfound.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_success_returns_token_without_sensitive_fields() throws Exception {
        String body = mockMvc.perform(post("/api/v1/session/wechat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"valid-code\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists())
            .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertTrue(json.path("data").path("token").asText().length() > 0);
        assertFalse(body.contains("openid"));
        assertFalse(body.contains("session_key"));
    }

    @Test
    void login_invalid_code_returns_wechat_code_invalid() throws Exception {
        mockMvc.perform(post("/api/v1/session/wechat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"bad-code\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("WECHAT_CODE_INVALID"));
    }

    @Test
    void me_without_session_returns_session_expired() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    @Test
    void me_with_valid_session_returns_user() throws Exception {
        String token = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid-code\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
