package com.lostfound.bookmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.account.InMemoryUserStore;
import com.lostfound.listing.ItemPostStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookmarkTest {

    private static final String LOST_BODY =
        "{\"type\":\"LOST\",\"category\":\"校园卡\",\"title\":\"遗失校园卡\","
            + "\"publicDescription\":\"在图书馆附近遗失校园卡\",\"school\":\"南方大学\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private ItemPostStore postStore;

    @Autowired
    private BookmarkStore bookmarkStore;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
        bookmarkStore.clear();
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

    @Test
    void bookmark_published_returns_post() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String postId = createPublishedPost(owner);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.post.id").value(postId))
            .andExpect(jsonPath("$.data.post.status").value("PUBLISHED"));
    }

    @Test
    void bookmark_duplicate_is_idempotent() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String postId = createPublishedPost(owner);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/bookmarks").header("Authorization", bearer(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));
    }

    @Test
    void bookmark_nonvisible_returns_404() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String postId = createPublishedPost(owner);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/close").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void bookmark_nonexistent_returns_404() throws Exception {
        String user = loginToken("valid-code-2");

        mockMvc.perform(post("/api/v1/posts/nonexistent-id/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void list_returns_bookmark_with_status() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String postId = createPublishedPost(owner);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/bookmarks").header("Authorization", bearer(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].postId").value(postId))
            .andExpect(jsonPath("$.data.items[0].post.status").value("PUBLISHED"));
    }

    @Test
    void list_is_isolated_between_users() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String other = loginToken("valid-code-3");
        String postId = createPublishedPost(owner);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/bookmarks").header("Authorization", bearer(other)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void unbookmark_removes() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String postId = createPublishedPost(owner);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/bookmarks").header("Authorization", bearer(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void unbookmark_idempotent() throws Exception {
        String user = loginToken("valid-code-2");

        mockMvc.perform(delete("/api/v1/posts/some-post/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());
    }

    @Test
    void bookmark_survives_post_close_with_status() throws Exception {
        String owner = confirmedToken("valid-code");
        String user = loginToken("valid-code-2");
        String postId = createPublishedPost(owner);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/bookmark").header("Authorization", bearer(user)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/" + postId + "/close").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/bookmarks").header("Authorization", bearer(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].post.status").value("CLOSED"));
    }
}
