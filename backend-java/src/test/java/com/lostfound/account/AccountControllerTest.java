package com.lostfound.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private AgreementGuard agreementGuard;

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
        String token = loginToken();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void login_returns_agreement_accepted_false() throws Exception {
        mockMvc.perform(post("/api/v1/session/wechat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"valid-code\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agreementAccepted").value(false));
    }

    @Test
    void confirm_agreement_then_me_returns_true() throws Exception {
        String token = loginToken();
        String header = "Bearer " + token;

        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agreementAccepted").value(true));

        mockMvc.perform(get("/api/v1/me").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agreementAccepted").value(true));
    }

    @Test
    void confirm_agreement_is_idempotent() throws Exception {
        String token = loginToken();
        String header = "Bearer " + token;

        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agreementAccepted").value(true));
    }

    @Test
    void guard_rejects_unconfirmed_user() {
        UserAccount user = userStore.findByOpenid("openid_test_001");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> agreementGuard.requireAgreed(user.id()));
        assertEquals(ErrorCode.AGREEMENT_NOT_ACCEPTED, ex.code());
    }

    @Test
    void guard_allows_confirmed_user() {
        UserAccount user = userStore.findByOpenid("openid_test_001");
        userStore.confirmAgreement(user.id());
        assertEquals(user.id(), agreementGuard.requireAgreed(user.id()).id());
    }
}
