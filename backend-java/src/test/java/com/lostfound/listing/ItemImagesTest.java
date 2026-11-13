package com.lostfound.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.abuse.RateLimiter;
import com.lostfound.account.InMemoryUserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItemImagesTest {

    private static final String CREATE_BODY =
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
    private ItemImageStore imageStore;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void resetStores() {
        userStore.clear();
        postStore.clear();
        imageStore.clear();
        rateLimiter.clear();
    }

    private String confirmedToken(String code) throws Exception {
        String token = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();
        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        return token;
    }

    private String createDraft(String token) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    private String upload(String token, String postId, String name, String contentType, byte[] bytes)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", name, contentType, bytes);
        return objectMapper.readTree(
                mockMvc.perform(multipart("/api/v1/posts/" + postId + "/images").file(file)
                        .header("Authorization", bearer(token)))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void upload_success_and_draft_detail_includes_image() throws Exception {
        String token = confirmedToken("valid-code");
        String postId = createDraft(token);

        MockMultipartFile file = new MockMultipartFile("file", "card.jpg", "image/jpeg",
            new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/posts/" + postId + "/images").file(file)
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").isNotEmpty())
            .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/media/")))
            .andExpect(jsonPath("$.data.sortOrder").value(0));

        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.images.length()").value(1));
    }

    @Test
    void upload_bad_type_returns_validation_failed() throws Exception {
        String token = confirmedToken("valid-code");
        String postId = createDraft(token);

        MockMultipartFile file = new MockMultipartFile("file", "card.gif", "image/gif",
            new byte[]{1});
        mockMvc.perform(multipart("/api/v1/posts/" + postId + "/images").file(file)
                .header("Authorization", bearer(token)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void upload_oversize_returns_validation_failed() throws Exception {
        String token = confirmedToken("valid-code");
        String postId = createDraft(token);

        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg",
            new byte[10 * 1024 * 1024 + 1]);
        mockMvc.perform(multipart("/api/v1/posts/" + postId + "/images").file(file)
                .header("Authorization", bearer(token)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void upload_by_non_author_returns_not_found() throws Exception {
        String owner = confirmedToken("valid-code");
        String other = confirmedToken("valid-code-2");
        String postId = createDraft(owner);

        MockMultipartFile file = new MockMultipartFile("file", "card.jpg", "image/jpeg",
            new byte[]{1});
        mockMvc.perform(multipart("/api/v1/posts/" + postId + "/images").file(file)
                .header("Authorization", bearer(other)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void upload_to_published_returns_invalid_state_transition() throws Exception {
        String token = confirmedToken("valid-code");
        String postId = createDraft(token);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile("file", "card.jpg", "image/jpeg",
            new byte[]{1});
        mockMvc.perform(multipart("/api/v1/posts/" + postId + "/images").file(file)
                .header("Authorization", bearer(token)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void draft_hidden_to_non_author_and_public_visible_with_images() throws Exception {
        String owner = confirmedToken("valid-code");
        String other = confirmedToken("valid-code-2");
        String postId = createDraft(owner);
        upload(owner, postId, "card.jpg", "image/jpeg", new byte[]{1});

        // 草稿对非作者不可见
        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(other)))
            .andExpect(status().isNotFound());

        // 发布后公开，图片对所有人可见
        mockMvc.perform(post("/api/v1/posts/" + postId + "/publish").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(other)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.images.length()").value(1));

        mockMvc.perform(get("/api/v1/posts").header("Authorization", bearer(other)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].images.length()").value(1));
    }

    @Test
    void delete_image_success() throws Exception {
        String token = confirmedToken("valid-code");
        String postId = createDraft(token);
        String imageId = upload(token, postId, "card.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(delete("/api/v1/posts/" + postId + "/images/" + imageId)
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.images.length()").value(0));
    }

    @Test
    void delete_by_non_author_returns_not_found() throws Exception {
        String owner = confirmedToken("valid-code");
        String other = confirmedToken("valid-code-2");
        String postId = createDraft(owner);
        String imageId = upload(owner, postId, "card.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(delete("/api/v1/posts/" + postId + "/images/" + imageId)
                .header("Authorization", bearer(other)))
            .andExpect(status().isNotFound());
    }
}
