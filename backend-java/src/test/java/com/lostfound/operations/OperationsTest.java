package com.lostfound.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.account.AccountService;
import com.lostfound.account.InMemoryUserStore;
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
class OperationsTest {

    private static final String LOST_BODY =
        "{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"遗失校园卡\","
            + "\"publicDescription\":\"在图书馆附近遗失校园卡\",\"schoolId\":\"018c0000-0000-7000-8000-000000000001\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ItemPostStore postStore;

    @Autowired
    private ReportStore reportStore;

    @Autowired
    private AuditStore auditStore;

    @BeforeEach
    void reset() {
        userStore.clear();
        accountService.seedOperator();
        postStore.clear();
        reportStore.clear();
        auditStore.clear();
    }

    private String loginToken(String code) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String operatorToken() throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
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

    private String createPublishedPost(String token) throws Exception {
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(LOST_BODY))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        return id;
    }

    private String userId(String token) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    @Test
    void non_operator_rejected() throws Exception {
        String user = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/admin/users/some-user/restrict").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"违规\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("OPERATOR_REQUIRED"));

        mockMvc.perform(get("/api/v1/admin/audit-events").header("Authorization", bearer(user)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("OPERATOR_REQUIRED"));
    }

    @Test
    void operator_removes_published_post_and_hides_from_search() throws Exception {
        String operator = operatorToken();
        String owner = confirmedToken("valid-code");
        String postId = createPublishedPost(owner);

        mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/remove").header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"违规信息\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REMOVED"));

        mockMvc.perform(get("/api/v1/posts").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void operator_remove_nonexistent_returns_404() throws Exception {
        String operator = operatorToken();

        mockMvc.perform(post("/api/v1/admin/posts/nonexistent/remove").header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void operator_remove_nonpublic_returns_409() throws Exception {
        String operator = operatorToken();
        String owner = confirmedToken("valid-code");
        String postId = createPublishedPost(owner);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/close").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/remove").header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void operator_restrict_then_blocked_from_publish_and_unrestrict() throws Exception {
        String operator = operatorToken();
        String target = confirmedToken("valid-code-2");
        String targetId = userId(target);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/restrict")
                .header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"违规\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RESTRICTED"));

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(target))
                .contentType(MediaType.APPLICATION_JSON).content(LOST_BODY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCOUNT_RESTRICTED"));

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/unrestrict")
                .header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"解除\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(target))
                .contentType(MediaType.APPLICATION_JSON).content(LOST_BODY))
            .andExpect(status().isOk());
    }

    @Test
    void operator_restrict_nonexistent_returns_404() throws Exception {
        String operator = operatorToken();

        mockMvc.perform(post("/api/v1/admin/users/nonexistent/restrict")
                .header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void operator_list_all_reports() throws Exception {
        String operator = operatorToken();
        String reporter = confirmedToken("valid-code-2");
        mockMvc.perform(post("/api/v1/reports").header("Authorization", bearer(reporter))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":\"post-1\",\"reason\":\"疑似诈骗\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/reports").header("Authorization", bearer(operator)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].reporterId").isNotEmpty());
    }

    @Test
    void operator_actions_write_audit_events() throws Exception {
        String operator = operatorToken();
        String owner = confirmedToken("valid-code");
        String postId = createPublishedPost(owner);

        mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/remove")
                .header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"违规\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/audit-events").header("Authorization", bearer(operator)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].eventType").value("POST_REMOVED"))
            .andExpect(jsonPath("$.data.items[0].metadata.reason").value("违规"));
    }

    @Test
    void operator_action_empty_reason_returns_422() throws Exception {
        String operator = operatorToken();

        mockMvc.perform(post("/api/v1/admin/users/some-user/restrict")
                .header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}
