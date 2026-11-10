package com.lostfound.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CampusProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @BeforeEach
    void resetStore() {
        userStore.clear();
    }

    private String loginToken() throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(post("/api/v1/session/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid-code\"}"))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void get_campus_profile_unset_returns_null_and_marker() throws Exception {
        String token = loginToken();

        String body = mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        assertTrue(data.path("school").isNull());
        assertTrue(data.path("campus").isNull());
        assertEquals("SELF_DECLARED", data.path("verificationStatus").asText());
    }

    @Test
    void put_then_get_campus_profile_returns_values() throws Exception {
        String token = loginToken();
        String header = bearer(token);

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"school\":\"南方大学\",\"campus\":\"东校区\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.school").value("南方大学"))
            .andExpect(jsonPath("$.data.campus").value("东校区"))
            .andExpect(jsonPath("$.data.verificationStatus").value("SELF_DECLARED"));

        mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.school").value("南方大学"))
            .andExpect(jsonPath("$.data.campus").value("东校区"));
    }

    @Test
    void put_empty_school_returns_validation_failed() throws Exception {
        String token = loginToken();

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"school\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void put_missing_school_returns_validation_failed() throws Exception {
        String token = loginToken();

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campus\":\"东校区\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void campus_profile_does_not_imply_certification() throws Exception {
        String token = loginToken();

        String body = mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"school\":\"南方大学\"}"))
            .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("verified"));
        assertFalse(body.contains("certified"));
    }

    @Test
    void update_campus_preserves_agreement() throws Exception {
        String token = loginToken();
        String header = bearer(token);

        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"school\":\"南方大学\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agreementAccepted").value(true));
    }

    @Test
    void confirm_agreement_preserves_campus() throws Exception {
        String token = loginToken();
        String header = bearer(token);

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"school\":\"南方大学\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.school").value("南方大学"));
    }
}
