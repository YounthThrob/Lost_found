package com.lostfound.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.abuse.RateLimiter;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommunicationTest {

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
    private BlockStore blockStore;

    @Autowired
    private MessageStore messageStore;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void reset() {
        userStore.clear();
        postStore.clear();
        claimStore.clear();
        blockStore.clear();
        messageStore.clear();
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

    private String acceptedClaim(String owner, String applicant) throws Exception {
        String postId = createPublishedPost(owner);
        String claimId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的校园卡\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
        return claimId;
    }

    @Test
    void block_and_unblock_lifecycle() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String applicantId = userId(applicant);
        String ownerId = userId(owner);

        mockMvc.perform(post("/api/v1/blocks").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockedUserId\":\"" + applicantId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.blockedUserId").value(applicantId));

        mockMvc.perform(post("/api/v1/blocks").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockedUserId\":\"" + applicantId + "\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/blocks").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockedUserId\":\"" + ownerId + "\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/blocks").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockedUserId\":\"nonexistent\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v1/me/blocks").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));

        mockMvc.perform(delete("/api/v1/blocks/" + applicantId).header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/blocks").header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(0));
    }

    @Test
    void message_send_list_and_blocked() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String applicantId = userId(applicant);
        String claimId = acceptedClaim(owner, applicant);

        mockMvc.perform(post("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"content\":\"明天下午3点方便吗\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("TEXT"))
            .andExpect(jsonPath("$.data.senderId").value(applicantId));

        mockMvc.perform(get("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pagination.total").value(1));

        mockMvc.perform(post("/api/v1/blocks").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockedUserId\":\"" + applicantId + "\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"content\":\"还在吗\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("BLOCKED"));

        mockMvc.perform(delete("/api/v1/blocks/" + applicantId).header("Authorization", bearer(owner)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"SUGGEST_TIME\",\"content\":\"明天下午3点\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("SUGGEST_TIME"));
    }

    @Test
    void message_authorization_and_validation() throws Exception {
        String owner = confirmedToken("valid-code");
        String applicant = confirmedToken("valid-code-2");
        String third = confirmedToken("valid-code-3");
        String claimId = acceptedClaim(owner, applicant);

        mockMvc.perform(get("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(third)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        String postId = createPublishedPost(owner);
        String unacceptedClaim = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/posts/" + postId + "/claims").header("Authorization", bearer(third))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statement\":\"我的\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/conversations/" + unacceptedClaim + "/messages")
                .header("Authorization", bearer(third))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"content\":\"hi\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INVALID\",\"content\":\"x\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/conversations/" + claimId + "/messages")
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"content\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}
