package com.lostfound.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.abuse.RateLimiter;
import com.lostfound.account.InMemoryUserStore;
import com.lostfound.listing.ItemPostStore;
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
class ContentModerationTest {

    private static final String CLEAN_BODY =
        "{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"遗失校园卡\","
            + "\"publicDescription\":\"在图书馆附近遗失一张校园卡\",\"schoolId\":\"018c0000-0000-7000-8000-000000000001\"}";
    private static final String BANNED_BODY =
        "{\"type\":\"LOST\",\"category\":\"其他\",\"title\":\"高价回收\","
            + "\"publicDescription\":\"要求先转账到指定账户\",\"schoolId\":\"018c0000-0000-7000-8000-000000000001\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private ItemPostStore postStore;

    @Autowired
    private ReportStore reportStore;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
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

    private String createDraft(String token, String body) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    @Test
    void publish_banned_keyword_returns_rejected() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, BANNED_BODY);

        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void publish_clean_returns_published() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CLEAN_BODY);

        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void rejected_post_hidden_from_search() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, BANNED_BODY);
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void rejected_post_visible_to_author_only() throws Exception {
        String token1 = confirmedToken("valid-code");
        String token2 = confirmedToken("valid-code-2");
        String id = createDraft(token1, BANNED_BODY);
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token1)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token2)))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_report_returns_submitted() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":\"post-abc123\",\"reason\":\"疑似诈骗\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_report_invalid_type_returns_422() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"OTHER\",\"targetId\":\"post-abc123\",\"reason\":\"其他\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void create_report_empty_reason_returns_422() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":\"post-abc123\",\"reason\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void list_my_reports_returns_own_only() throws Exception {
        String token1 = confirmedToken("valid-code");
        String token2 = confirmedToken("valid-code-2");

        mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(token1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":\"post-abc123\",\"reason\":\"疑似诈骗\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/me").header("Authorization", bearer(token1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));

        mockMvc.perform(get("/api/v1/reports/me").header("Authorization", bearer(token2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }
}
