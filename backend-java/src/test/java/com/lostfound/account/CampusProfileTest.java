package com.lostfound.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.directory.Campus;
import com.lostfound.directory.DirectoryService;
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

    private static final String SCHOOL_ID = "018c0000-0000-7000-8000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @Autowired
    private DirectoryService directoryService;

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

    /** 通过目录服务按校区名取到确定学校下的校区 id（校区主键为不可预测 UUID）。 */
    private String activeCampusId(String name) {
        return directoryService.listCampuses(SCHOOL_ID).stream()
            .filter(c -> name.equals(c.name()))
            .map(Campus::id)
            .findFirst()
            .orElse(null);
    }

    @Test
    void get_campus_profile_unset_returns_null_and_marker() throws Exception {
        String token = loginToken();

        String body = mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        assertTrue(data.path("schoolId").isNull());
        assertTrue(data.path("campusId").isNull());
        assertEquals("SELF_DECLARED", data.path("verificationStatus").asText());
    }

    @Test
    void put_then_get_campus_profile_returns_values() throws Exception {
        String token = loginToken();
        String header = bearer(token);
        String campusId = activeCampusId("东校区");
        assertTrue(campusId != null && !campusId.isEmpty());

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"schoolId\":\"" + SCHOOL_ID + "\",\"campusId\":\"" + campusId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.schoolName").value("南方大学"))
            .andExpect(jsonPath("$.data.campusName").value("东校区"))
            .andExpect(jsonPath("$.data.verificationStatus").value("SELF_DECLARED"));

        mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.schoolName").value("南方大学"))
            .andExpect(jsonPath("$.data.campusName").value("东校区"));
    }

    @Test
    void put_empty_school_returns_validation_failed() throws Exception {
        String token = loginToken();

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"schoolId\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void put_missing_school_returns_validation_failed() throws Exception {
        String token = loginToken();

        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campusId\":\"any-campus-id\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void campus_profile_does_not_imply_certification() throws Exception {
        String token = loginToken();

        String body = mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"schoolId\":\"" + SCHOOL_ID + "\"}"))
            .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("certified"));
        assertFalse(body.contains("officiallyVerified"));
    }

    @Test
    void update_campus_preserves_agreement() throws Exception {
        String token = loginToken();
        String header = bearer(token);

        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/me/campus-profile").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"schoolId\":\"" + SCHOOL_ID + "\"}"))
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
                .content("{\"schoolId\":\"" + SCHOOL_ID + "\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/me/agreement").header("Authorization", header))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/campus-profile").header("Authorization", header))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.schoolName").value("南方大学"));
    }
}
