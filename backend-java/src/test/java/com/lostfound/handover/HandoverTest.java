package com.lostfound.handover;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.account.InMemoryUserStore;
import com.lostfound.claim.ClaimStore;
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
class HandoverTest {

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

    @Autowired
    private HandoverStore handoverStore;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
        claimStore.clear();
        handoverStore.clear();
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

    private String createPublishedPost(String token) throws Exception {
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(FOUND_BODY))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/posts/" + id + "/publish").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        return id;
    }

    private String submitClaim(String applicant, String postId) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    private String acceptedClaim(String owner, String applicant) throws Exception {
        String postId = createPublishedPost(owner);
        String claimId = submitClaim(applicant, postId);
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
        return claimId;
    }

    private String createHandover(String token, String claimId) throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"明天下午3点\","
                            + "\"publicPlace\":\"图书馆一楼\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
    }

    @Test
    void create_returns_pending() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String ownerId = userId(owner);
        String claimId = acceptedClaim(owner, applicant);

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"明天下午3点\","
                    + "\"publicPlace\":\"图书馆一楼\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.proposerId").value(ownerId))
            .andExpect(jsonPath("$.data.ownerConfirmedAt").isEmpty())
            .andExpect(jsonPath("$.data.claimantConfirmedAt").isEmpty());
    }

    @Test
    void duplicate_create_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String claimId = acceptedClaim(owner, applicant);

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"明天下午3点\","
                    + "\"publicPlace\":\"图书馆一楼\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"后天\","
                    + "\"publicPlace\":\"校门口\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("HANDOVER_ALREADY_EXISTS"));
    }

    @Test
    void non_participant_create_returns_404() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String third = confirmedToken("valid-code-3");
        String claimId = acceptedClaim(owner, applicant);

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(third))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"明天\","
                    + "\"publicPlace\":\"校门口\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void create_on_unaccepted_claim_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner);
        String claimId = submitClaim(applicant, postId);

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"明天\","
                    + "\"publicPlace\":\"校门口\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void create_empty_field_returns_422() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String claimId = acceptedClaim(owner, applicant);

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"\","
                    + "\"publicPlace\":\"校门口\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void both_confirm_completes_and_couples_state() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner);
        String claimId = submitClaim(applicant, postId);
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
        String handoverId = createHandover(owner, claimId);

        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/confirm").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.ownerConfirmedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.claimantConfirmedAt").isEmpty());

        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/confirm").header("Authorization", bearer(applicant)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.ownerConfirmedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.claimantConfirmedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void repeat_confirm_is_idempotent() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String claimId = acceptedClaim(owner, applicant);
        String handoverId = createHandover(owner, claimId);

        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/confirm").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/confirm").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void cancel_returns_cancelled_and_post_published() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String postId = createPublishedPost(owner);
        String claimId = submitClaim(applicant, postId);
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
        String handoverId = createHandover(owner, claimId);

        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/cancel").header("Authorization", bearer(applicant)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/posts/" + postId).header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void confirm_on_cancelled_returns_409() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String claimId = acceptedClaim(owner, applicant);
        String handoverId = createHandover(owner, claimId);
        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/cancel").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/confirm").header("Authorization", bearer(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void non_participant_confirm_returns_404() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String third = confirmedToken("valid-code-3");
        String claimId = acceptedClaim(owner, applicant);
        String handoverId = createHandover(owner, claimId);

        mockMvc.perform(post("/api/v1/handovers/" + handoverId + "/confirm").header("Authorization", bearer(third)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void create_without_agreement_returns_403() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String claimId = acceptedClaim(owner, applicant);
        String nonConfirmedApplicant = loginToken("valid-code-3");

        mockMvc.perform(post("/api/v1/handovers").header("Authorization", bearer(nonConfirmedApplicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimId\":\"" + claimId + "\",\"timeWindow\":\"明天\","
                    + "\"publicPlace\":\"校门口\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("AGREEMENT_NOT_ACCEPTED"));
    }
}
