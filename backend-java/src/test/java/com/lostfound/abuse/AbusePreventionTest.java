package com.lostfound.abuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.account.InMemoryUserStore;
import com.lostfound.claim.ClaimStore;
import com.lostfound.communication.BlockStore;
import com.lostfound.communication.MessageStore;
import com.lostfound.listing.ItemPostStore;
import com.lostfound.moderation.ReportStore;
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
class AbusePreventionTest {

    private static final String POST_BODY =
        "{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"遗失校园卡\","
            + "\"publicDescription\":\"在图书馆附近遗失一张校园卡\",\"schoolId\":\"018c0000-0000-7000-8000-000000000001\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private ItemPostStore postStore;

    @Autowired
    private ClaimStore claimStore;

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private MessageStore messageStore;

    @Autowired
    private ReportStore reportStore;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
        claimStore.clear();
        blockStore.clear();
        messageStore.clear();
        reportStore.clear();
        rateLimiter.clear();
    }

    private String loginToken(String code) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String confirmedToken(String code) throws Exception {
        String token = loginToken(code);
        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String userId(String token) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    @Test
    void claim_rate_limit_returns_429() throws Exception {
        String user = confirmedToken("valid-code-4");
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/posts/dummy-post/claims").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"x\"}"))
                .andExpect(status().isNotFound());
        }
        mockMvc.perform(post("/api/v1/posts/dummy-post/claims").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"x\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("CLAIM_RATE_LIMITED"));
    }

    @Test
    void post_rate_limit_returns_429() throws Exception {
        String user = confirmedToken("valid-code-4");
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void message_rate_limit_returns_429() throws Exception {
        String user = confirmedToken("valid-code-4");
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/v1/conversations/dummy/messages").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"TEXT\",\"content\":\"hi\"}"))
                .andExpect(status().isNotFound());
        }
        mockMvc.perform(post("/api/v1/conversations/dummy/messages").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"TEXT\",\"content\":\"hi\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void report_rate_limit_returns_429() throws Exception {
        String user = confirmedToken("valid-code-4");
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"targetType\":\"POST\",\"targetId\":\"post-x\",\"reason\":\"疑似诈骗\"}"))
                .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":\"post-x\",\"reason\":\"疑似诈骗\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void different_operations_count_independently() throws Exception {
        String user = confirmedToken("valid-code-4");
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/posts/dummy-post/claims").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"x\"}"))
                .andExpect(status().isNotFound());
        }
        // 认领已到上限，但发布不受影响
        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
            .andExpect(status().isOk());
    }

    @Test
    void restricted_account_is_rejected() throws Exception {
        String user = confirmedToken("valid-code");
        String id = userId(user);
        userStore.restrict(id);

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCOUNT_RESTRICTED"));
    }

    @Test
    void normal_account_is_unaffected() throws Exception {
        String user = confirmedToken("valid-code");
        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
            .andExpect(status().isOk());
    }

    @Test
    void error_response_does_not_leak_wind_control_details() throws Exception {
        String user = confirmedToken("valid-code-4");
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/posts/dummy-post/claims").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"x\"}"))
                .andExpect(status().isNotFound());
        }
        String body = mockMvc.perform(post("/api/v1/posts/dummy-post/claims").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"x\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("CLAIM_RATE_LIMITED"))
            .andExpect(jsonPath("$.error.message").value("认领申请过于频繁，请稍后再试"))
            .andExpect(jsonPath("$.error.details").isEmpty())
            .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("window"));
    }
}
