package com.integral.mall.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminLoginAndMeShouldWork() throws Exception {
        String loginResp = mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"admin123456\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginNode = objectMapper.readTree(loginResp);
        assertEquals(0, loginNode.get("code").asInt());
        String token = loginNode.get("data").get("access_token").asText();
        assertTrue(token.startsWith("admin-token-"));

        String meResp = mockMvc.perform(
                        get("/api/v1/admin/auth/me")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode meNode = objectMapper.readTree(meResp);
        assertEquals(0, meNode.get("code").asInt());
        assertEquals("admin", meNode.get("data").get("username").asText());
    }

    @Test
    void otherAdminLoginShouldWork() throws Exception {
        String loginResp = mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"ops_lead\",\"password\":\"ops_lead@123\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginNode = objectMapper.readTree(loginResp);
        assertEquals(0, loginNode.get("code").asInt());
        assertEquals("ops_lead", loginNode.get("data").get("admin_user").get("username").asText());
    }

    @Test
    void adminLoginPreflightShouldPassCors() throws Exception {
        mockMvc.perform(options("/api/v1/admin/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }
}
