package com.lostfound.claim;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimTest {

    private static final String FOUND_BODY =
        "{\"type\":\"FOUND\",\"category\":\"校园卡\",\"title\":\"拾到校园卡\","
            + "\"publicDescription\":\"在食堂拾到一张校园卡\",\"school\":\"南方大学\"}";
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

    @Test
    void submit_claim_returns_submitted() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"我的校园卡，学号尾号 0427\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void submit_claim_without_agreement_returns_403() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = loginToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"我的校园卡\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("AGREEMENT_NOT_ACCEPTED"));
    }

    @Test
    void submit_claim_on_lost_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, LOST_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"我的校园卡\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("POST_NOT_CLAIMABLE"));
    }

    @Test
    void submit_claim_on_own_post_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String postId = createPublishedPost(owner, FOUND_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"这是我自己的信息\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("POST_NOT_CLAIMABLE"));
    }

    @Test
    void submit_duplicate_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"我的校园卡\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"再次申请\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CLAIM_DUPLICATE"));
    }

    @Test
    void submit_empty_statement_returns_422() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void owner_lists_claims_and_non_owner_cannot() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"我的校园卡\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1))
            .andExpect(jsonPath("$.data.items[0].statement").value("我的校园卡"));

        mockMvc.perform(get("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void request_info_returns_needs_more_info() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);
        String claimId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/request-info").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("NEEDS_MORE_INFO"));
    }

    @Test
    void request_info_on_accepted_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);
        String claimId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/request-info").header("Authorization", bearer(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void accept_returns_accepted_and_post_pending_handover() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);
        String claimId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_HANDOVER"));
    }

    @Test
    void reject_returns_rejected_and_post_back_to_published() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);
        String claimId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/reject").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(applicant)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void accept_second_claim_returns_handover_conflict() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant1 = confirmedToken("valid-code-2");
        String applicant2 = confirmedToken("valid-code-3");
        String postId = createPublishedPost(owner, FOUND_BODY);

        String claim1 = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡A\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        String claim2 = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡B\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/claims/" + claim1 + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/claims/" + claim2 + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("HANDOVER_CONFLICT"));
    }

    @Test
    void non_owner_cannot_accept() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner, FOUND_BODY);
        String claimId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(applicant)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
