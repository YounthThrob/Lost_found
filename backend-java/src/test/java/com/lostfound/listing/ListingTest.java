package com.lostfound.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.abuse.RateLimiter;
import com.lostfound.account.InMemoryUserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ListingTest {

    private static final String CREATE_BODY =
        "{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"遗失校园卡\","
            + "\"publicDescription\":\"在图书馆附近遗失一张校园卡\",\"school\":\"南方大学\","
            + "\"campus\":\"东校区\",\"area\":\"图书馆\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private ItemPostStore postStore;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void resetStores() {
        userStore.clear();
        postStore.clear();
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

    private String createDraft(String token, String body) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void create_draft_returns_draft_status() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.type").value("LOST"))
            .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_without_agreement_returns_403() throws Exception {
        String token = loginToken("valid-code");

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("AGREEMENT_NOT_ACCEPTED"));
    }

    @Test
    void create_empty_title_returns_422() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"\","
                    + "\"publicDescription\":\"描述\",\"school\":\"南方大学\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void create_invalid_type_returns_422() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"OTHER\",\"category\":\"校园卡\",\"title\":\"标题\","
                    + "\"publicDescription\":\"描述\",\"school\":\"南方大学\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void edit_draft_success() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CREATE_BODY);

        mockMvc.perform(patch("/api/v1/posts/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"FOUND\",\"category\":\"雨伞\",\"title\":\"拾到雨伞\","
                    + "\"publicDescription\":\"在食堂拾到一把雨伞\",\"school\":\"南方大学\",\"campus\":\"西校区\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("FOUND"))
            .andExpect(jsonPath("$.data.title").value("拾到雨伞"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void edit_published_returns_409() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CREATE_BODY);
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/posts/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"改标题\","
                    + "\"publicDescription\":\"描述\",\"school\":\"南方大学\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void edit_others_post_returns_404() throws Exception {
        String token1 = confirmedToken("valid-code");
        String token2 = confirmedToken("valid-code-2");
        String id = createDraft(token1, CREATE_BODY);

        mockMvc.perform(patch("/api/v1/posts/" + id).header("Authorization", bearer(token2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"改标题\","
                    + "\"publicDescription\":\"描述\",\"school\":\"南方大学\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void publish_then_detail_returns_published() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CREATE_BODY);

        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.title").value("遗失校园卡"));
    }

    @Test
    void publish_again_returns_409() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CREATE_BODY);
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void close_draft_returns_409() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CREATE_BODY);

        mockMvc.perform(post("/api/v1/posts/" + id + "/close").header("Authorization", bearer(token)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void close_then_search_excludes() throws Exception {
        String token = confirmedToken("valid-code");
        String id = createDraft(token, CREATE_BODY);
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/" + id + "/close").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/posts").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void search_only_published_and_filters() throws Exception {
        String token = confirmedToken("valid-code");
        String draftId = createDraft(token, CREATE_BODY);

        String foundBody = "{\"type\":\"FOUND\",\"category\":\"雨伞\",\"title\":\"拾到雨伞\","
            + "\"publicDescription\":\"在食堂拾到一把雨伞\",\"school\":\"南方大学\",\"campus\":\"东校区\"}";
        String foundId = createDraft(token, foundBody);
        mockMvc.perform(post("/api/v1/posts/" + foundId + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        // 草稿不出现在搜索中
        mockMvc.perform(get("/api/v1/posts").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(foundId))
            .andExpect(jsonPath("$.data.items[0].type").value("FOUND"));

        // 类型筛选
        mockMvc.perform(get("/api/v1/posts").param("type", "FOUND").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));

        mockMvc.perform(get("/api/v1/posts").param("type", "LOST").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));

        // 关键词筛选
        mockMvc.perform(get("/api/v1/posts").param("keyword", "雨伞").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));

        assertTrue(draftId != null);
    }

    @Test
    void search_pagination() throws Exception {
        String token = confirmedToken("valid-code");
        for (int i = 0; i < 3; i++) {
            String body = "{\"type\":\"LOST\",\"category\":\"其他\",\"title\":\"物品" + i + "\","
                + "\"publicDescription\":\"描述\",\"school\":\"南方大学\"}";
            String id = createDraft(token, body);
            mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/posts").param("page", "1").param("pageSize", "2")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.page").value(1))
            .andExpect(jsonPath("$.data.pagination.pageSize").value(2))
            .andExpect(jsonPath("$.data.pagination.total").value(3))
            .andExpect(jsonPath("$.data.pagination.hasMore").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void search_invalid_page_returns_422() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(get("/api/v1/posts").param("page", "0").header("Authorization", bearer(token)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void search_non_integer_page_returns_422() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(get("/api/v1/posts").param("page", "abc").header("Authorization", bearer(token)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void detail_draft_only_author_visible() throws Exception {
        String token1 = confirmedToken("valid-code");
        String token2 = confirmedToken("valid-code-2");
        String id = createDraft(token1, CREATE_BODY);

        mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token2)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void detail_not_found_returns_404() throws Exception {
        String token = confirmedToken("valid-code");

        mockMvc.perform(get("/api/v1/posts/nonexistent-id").header("Authorization", bearer(token)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void published_detail_visible_to_other_user() throws Exception {
        String token1 = confirmedToken("valid-code");
        String token2 = confirmedToken("valid-code-2");
        String id = createDraft(token1, CREATE_BODY);
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token1)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        JsonNode data = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/posts/" + id).header("Authorization", bearer(token2)))
                    .andReturn().getResponse().getContentAsString())
            .path("data");
        assertEquals("PUBLISHED", data.path("status").asText());
    }
}
