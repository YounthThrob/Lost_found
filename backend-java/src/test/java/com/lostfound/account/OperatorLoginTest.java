package com.lostfound.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class OperatorLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private AccountService accountService;

    @BeforeEach
    void resetStore() {
        userStore.clear();
        accountService.seedOperator();
    }

    private String operatorToken() throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    @Test
    void operator_login_success_returns_token() throws Exception {
        mockMvc.perform(post("/api/v1/session/operator")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.agreementAccepted").doesNotExist());
    }

    @Test
    void operator_login_does_not_leak_password_or_hash() throws Exception {
        String body = mockMvc.perform(post("/api/v1/session/operator")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertTrue(json.path("data").path("token").asText().length() > 0);
        assertFalse(body.contains("admin123"));
        assertFalse(body.contains("passwordHash"));
        assertFalse(body.contains("$2a$"));
    }

    @Test
    void operator_login_wrong_password_returns_invalid_credentials() throws Exception {
        mockMvc.perform(post("/api/v1/session/operator")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void operator_login_wrong_username_returns_invalid_credentials() throws Exception {
        mockMvc.perform(post("/api/v1/session/operator")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"no-such\",\"password\":\"admin123\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void operator_token_can_access_operations() throws Exception {
        String token = operatorToken();
        mockMvc.perform(get("/api/v1/admin/audit-events").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
