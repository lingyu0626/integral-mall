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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAdminToken() throws Exception {
        String loginResp = mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"admin123456\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loginNode = objectMapper.readTree(loginResp);
        return loginNode.get("data").get("access_token").asText();
    }

    private long pickExchangeableSpuId() throws Exception {
        String productsResp = mockMvc.perform(
                        get("/api/v1/app/products")
                                .param("pageNo", "1")
                                .param("pageSize", "200")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode list = objectMapper.readTree(productsResp).get("data").get("list");
        long fallback = 101L;
        for (JsonNode item : list) {
            long id = item.get("id").asLong();
            long stock = item.path("stock_available").asLong(0);
            if (fallback == 101L) {
                fallback = id;
            }
            if (stock > 0) {
                return id;
            }
        }
        return fallback;
    }

    @Test
    void homeAndCatalogApisShouldReturnOk() throws Exception {
        String home = mockMvc.perform(get("/api/v1/app/home/recommends"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode homeNode = objectMapper.readTree(home);
        assertEquals(0, homeNode.get("code").asInt());
        assertTrue(homeNode.get("data").get("list").size() > 0);

        String categories = mockMvc.perform(get("/api/v1/app/categories"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode cateNode = objectMapper.readTree(categories);
        assertEquals(0, cateNode.get("code").asInt());
        assertTrue(cateNode.get("data").get("total").asInt() > 0);
    }

    @Test
    void loginExchangeOrderFlowShouldWork() throws Exception {
        String loginResp = mockMvc.perform(
                        post("/api/v1/app/auth/wx-login")
                                .header("X-Request-Id", "rid-test-login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"wx-code-demo\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginNode = objectMapper.readTree(loginResp);
        assertEquals(0, loginNode.get("code").asInt());
        String token = loginNode.get("data").get("access_token").asText();

        String adminToken = loginAdminToken();
        String adjustResp = mockMvc.perform(
                        post("/api/v1/admin/users/1001/points/adjust")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"adjust_point\":50000,\"adjust_remark\":\"test-topup\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(0, objectMapper.readTree(adjustResp).get("code").asInt());

        long spuId = pickExchangeableSpuId();
        String createAddressResp = mockMvc.perform(
                        post("/api/v1/app/addresses")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Request-Id", "rid-test-address-" + System.nanoTime())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"receiver_name\":\"测试用户\",\"receiver_phone\":\"13812345678\",\"province_name\":\"广东省\",\"city_name\":\"深圳市\",\"district_name\":\"南山区\",\"detail_address\":\"科技园 1 号\",\"is_default\":1,\"country_code\":\"CN\",\"status_code\":\"ACTIVE\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode addressNode = objectMapper.readTree(createAddressResp);
        assertEquals(0, addressNode.get("code").asInt());
        long addressId = addressNode.get("data").get("id").asLong();

        String exchangeResp = mockMvc.perform(
                        post("/api/v1/app/exchanges/orders")
                                .header("X-Request-Id", "rid-test-exchange-1")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"spu_id\":" + spuId + ",\"address_id\":" + addressId + ",\"quantity\":1}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode exchangeNode = objectMapper.readTree(exchangeResp);
        assertEquals(0, exchangeNode.get("code").asInt());
        assertTrue(exchangeNode.get("data").has("order_id"));

        String ordersResp = mockMvc.perform(
                        get("/api/v1/app/orders")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode ordersNode = objectMapper.readTree(ordersResp);
        assertEquals(0, ordersNode.get("code").asInt());
        assertTrue(ordersNode.get("data").get("list").size() > 0);
    }
}
