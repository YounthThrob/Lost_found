package com.lostfound.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.account.InMemoryUserStore;
import com.lostfound.claim.ClaimStore;
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
class MyItemsTest {

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
    private ClaimStore claimStore;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
        claimStore.clear();
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

    private String createDraft(String token) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(LOST_BODY))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    private String createPublishedFound(String token) throws Exception {
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(FOUND_BODY))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        return id;
    }

    @Test
    void my_posts_includes_draft_and_isolates() throws Exception {
        String owner = confirmedToken("valid-code");
        String other = confirmedToken("valid-code-2");
        createDraft(owner);

        mockMvc.perform(get("/api/v1/me/posts").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/me/posts").header("Authorization", bearer(other)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void my_claims_isolates() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedFound(owner);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims")
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/claims").header("Authorization", bearer(applicant)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));

        mockMvc.perform(get("/api/v1/me/claims").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }
}
