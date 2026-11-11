package com.lostfound.clue;

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
class ClueTest {

    private static final String LOST_BODY =
        "{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"遗失校园卡\","
            + "\"publicDescription\":\"在图书馆附近遗失校园卡\",\"school\":\"南方大学\"}";
    private static final String FOUND_BODY =
        "{\"type\":\"FOUND\",\"category\":\"校园卡\",\"title\":\"拾到校园卡\","
            + "\"publicDescription\":\"在食堂拾到一张校园卡\",\"school\":\"南方大学\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private ItemPostStore postStore;

    @Autowired
    private ClueStore clueStore;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
        clueStore.clear();
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

    private String createPublishedPost(String token, String body) throws Exception {
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        return id;
    }

    private String submitClue(String token, String postId, String content) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    @Test
    void submit_clue_on_lost_returns_submitted() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"我在图书馆门口看到过这张卡\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.data.providerId").isNotEmpty())
            .andExpect(jsonPath("$.data.content").value("我在图书馆门口看到过这张卡"));
    }

    @Test
    void submit_clue_without_agreement_returns_403() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = loginToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"一条线索\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("AGREEMENT_NOT_ACCEPTED"));
    }

    @Test
    void submit_clue_on_found_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"线索不该给招领\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CLUE_NOT_ALLOWED"));
    }

    @Test
    void submit_clue_on_closed_returns_404() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/close").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"线索不该给已关闭信息\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void submit_clue_banned_returns_rejected() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"先转账给我再告诉你\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void submit_empty_clue_returns_422() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void owner_lists_pending_clues_and_non_owner_cannot() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        submitClue(provider, postId, "我在图书馆门口看到过这张卡");

        mockMvc.perform(get("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].status").value("SUBMITTED"));

        mockMvc.perform(get("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(provider)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void rejected_clue_not_in_pending_list() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        submitClue(provider, postId, "先转账给我再告诉你");

        mockMvc.perform(get("/api/v1/posts/" + postId + "/clues").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void decide_valid_returns_accepted() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        String clueId = submitClue(provider, postId, "我在图书馆门口看到过这张卡");

        mockMvc.perform(post("/api/v1/clues/" + clueId + "/decide").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"VALID\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void decide_invalid_returns_closed() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        String clueId = submitClue(provider, postId, "我在图书馆门口看到过这张卡");

        mockMvc.perform(post("/api/v1/clues/" + clueId + "/decide").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"INVALID\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void decide_again_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        String clueId = submitClue(provider, postId, "我在图书馆门口看到过这张卡");
        mockMvc.perform(post("/api/v1/clues/" + clueId + "/decide").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"VALID\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/clues/" + clueId + "/decide").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"INVALID\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void decide_invalid_decision_returns_422() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        String clueId = submitClue(provider, postId, "我在图书馆门口看到过这张卡");

        mockMvc.perform(post("/api/v1/clues/" + clueId + "/decide").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"MAYBE\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void non_owner_cannot_decide() throws Exception {
        String owner = confirmedToken("valid-code");
        String provider = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);
        String clueId = submitClue(provider, postId, "我在图书馆门口看到过这张卡");

        mockMvc.perform(post("/api/v1/clues/" + clueId + "/decide").header("Authorization", bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"VALID\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
