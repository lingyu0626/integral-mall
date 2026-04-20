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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminBusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginToken() throws Exception {
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

    private String loginAppToken() throws Exception {
        String loginResp = mockMvc.perform(
                        post("/api/v1/app/auth/wx-login")
                                .header("X-Request-Id", "test-app-login-" + System.nanoTime())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"wx-code-test\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loginNode = objectMapper.readTree(loginResp);
        assertEquals(0, loginNode.get("code").asInt());
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
    void usersAndProductsApisShouldWork() throws Exception {
        String token = loginToken();

        String usersResp = mockMvc.perform(
                        get("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode usersNode = objectMapper.readTree(usersResp);
        assertEquals(0, usersNode.get("code").asInt());
        assertTrue(usersNode.get("data").get("total").asInt() > 0);

        String productsResp = mockMvc.perform(
                        get("/api/v1/admin/products/spu")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode productsNode = objectMapper.readTree(productsResp);
        assertEquals(0, productsNode.get("code").asInt());
        assertTrue(productsNode.get("data").get("list").size() > 0);
    }

    @Test
    void superAdminShouldViewAdminPassword() throws Exception {
        String token = loginToken();

        String viewPwdResp = mockMvc.perform(
                        get("/api/v1/admin/admin-users/1/password")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode viewPwdNode = objectMapper.readTree(viewPwdResp);
        assertEquals(0, viewPwdNode.get("code").asInt());
        assertTrue(viewPwdNode.get("data").get("password").asText().length() >= 6);

        String resetPwdResp = mockMvc.perform(
                        post("/api/v1/admin/admin-users/1/reset-password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode resetPwdNode = objectMapper.readTree(resetPwdResp);
        assertEquals(0, resetPwdNode.get("code").asInt());
        String tempPwd = resetPwdNode.get("data").get("temp_password").asText();
        assertTrue(tempPwd.length() >= 6);

        String viewAfterResetResp = mockMvc.perform(
                        get("/api/v1/admin/admin-users/1/password")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode viewAfterResetNode = objectMapper.readTree(viewAfterResetResp);
        assertEquals(0, viewAfterResetNode.get("code").asInt());
        assertEquals(tempPwd, viewAfterResetNode.get("data").get("password").asText());
    }

    @Test
    void userRemarkAndPointAdjustSearchShouldWork() throws Exception {
        String token = loginToken();
        long userId = 1001L;
        String userRemark = "高价值客诉用户";
        String orderRemark = "客服补发积分";

        String updateUserRemarkResp = mockMvc.perform(
                        put("/api/v1/admin/users/" + userId + "/remark")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"admin_remark\":\"" + userRemark + "\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(0, objectMapper.readTree(updateUserRemarkResp).get("code").asInt());

        String searchByUserRemarkResp = mockMvc.perform(
                        get("/api/v1/admin/users")
                                .param("keyword", "高价值客诉")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode userRemarkList = objectMapper.readTree(searchByUserRemarkResp).get("data").get("list");
        assertTrue(userRemarkList.size() > 0);
        assertEquals(userId, userRemarkList.get(0).get("id").asLong());

        String updateOrderRemarkResp = mockMvc.perform(
                        put("/api/v1/admin/orders/30001/remark")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"remark\":\"" + orderRemark + "\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(0, objectMapper.readTree(updateOrderRemarkResp).get("code").asInt());

        String searchByOrderRemarkResp = mockMvc.perform(
                        get("/api/v1/admin/users")
                                .param("keyword", "补发积分")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode orderRemarkList = objectMapper.readTree(searchByOrderRemarkResp).get("data").get("list");
        assertTrue(orderRemarkList.size() > 0);
        assertEquals(userId, orderRemarkList.get(0).get("id").asLong());

        String adjustResp = mockMvc.perform(
                        post("/api/v1/admin/users/" + userId + "/points/adjust")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"adjust_point\":120,\"adjust_remark\":\"客服补偿\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(0, objectMapper.readTree(adjustResp).get("code").asInt());
    }

    @Test
    void orderApproveFlowShouldWork() throws Exception {
        String token = loginToken();
        String listResp = mockMvc.perform(
                        get("/api/v1/admin/orders")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode orderList = objectMapper.readTree(listResp).get("data").get("list");
        long orderId = orderList.get(0).get("id").asLong();
        for (JsonNode item : orderList) {
            if ("PENDING_AUDIT".equals(item.get("order_status_code").asText())) {
                orderId = item.get("id").asLong();
                break;
            }
        }

        String approveResp = mockMvc.perform(
                        post("/api/v1/admin/orders/" + orderId + "/approve")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"audit_remark\":\"ok\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode approveNode = objectMapper.readTree(approveResp);
        int approveCode = approveNode.get("code").asInt();
        assertTrue(approveCode == 0 || approveCode == 4001);

        if (approveCode == 0) {
            String orderResp = mockMvc.perform(
                            get("/api/v1/admin/orders/" + orderId)
                                    .header("Authorization", "Bearer " + token)
                    ).andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode orderNode = objectMapper.readTree(orderResp);
            assertEquals("PENDING_SHIP", orderNode.get("data").get("order_status_code").asText());
        }

        String remarkResp = mockMvc.perform(
                        put("/api/v1/admin/orders/" + orderId + "/remark")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"remark\":\"测试备注\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode remarkNode = objectMapper.readTree(remarkResp);
        assertEquals(0, remarkNode.get("code").asInt());
    }

    @Test
    void groupResourceUpdateAndDeleteShouldNotBeOverwrittenByMiniSync() throws Exception {
        String token = loginToken();
        String createResp = mockMvc.perform(
                        post("/api/v1/admin/group-resources")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"group_name\":\"测试群资源\",\"qr_image_url\":\"data:image/png;base64,seed\",\"intro_text\":\"测试群资源\",\"max_member_count\":500,\"current_member_count\":0,\"expire_at\":\"2099-12-31 23:59:59\",\"status_code\":\"ENABLED\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createNode = objectMapper.readTree(createResp);
        assertEquals(0, createNode.get("code").asInt());
        long resourceId = createNode.get("data").get("id").asLong();

        String customQr = "data:image/png;base64,test-group-resource";
        String updateBody = "{"
                + "\"group_name\":\"测试群资源\","
                + "\"qr_image_url\":\"" + customQr + "\","
                + "\"intro_text\":\"测试群资源\","
                + "\"max_member_count\":500,"
                + "\"current_member_count\":0,"
                + "\"expire_at\":\"2099-12-31 23:59:59\","
                + "\"status_code\":\"ENABLED\""
                + "}";
        mockMvc.perform(
                        put("/api/v1/admin/group-resources/" + resourceId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                ).andExpect(status().isOk());

        // 触发一次列表（会执行 sync），再校验详情仍为后台修改值
        mockMvc.perform(
                        get("/api/v1/admin/group-resources")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk());

        String detailResp = mockMvc.perform(
                        get("/api/v1/admin/group-resources/" + resourceId)
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode detailNode = objectMapper.readTree(detailResp);
        assertEquals(customQr, detailNode.get("data").get("qr_image_url").asText());

        String appDetailResp = mockMvc.perform(
                        get("/api/v1/app/group-resources/" + resourceId)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode appDetailNode = objectMapper.readTree(appDetailResp);
        assertEquals(customQr, appDetailNode.get("data").get("qr_code_url").asText());

        mockMvc.perform(
                        delete("/api/v1/admin/group-resources/" + resourceId)
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk());

        String listAfterDeleteResp = mockMvc.perform(
                        get("/api/v1/admin/group-resources")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode listAfterDelete = objectMapper.readTree(listAfterDeleteResp).get("data").get("list");
        boolean exists = false;
        for (JsonNode item : listAfterDelete) {
            if (item.get("id").asLong() == resourceId) {
                exists = true;
                break;
            }
        }
        assertTrue(!exists);

        String appDeletedDetailResp = mockMvc.perform(
                        get("/api/v1/app/group-resources/" + resourceId)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode appDeletedDetailNode = objectMapper.readTree(appDeletedDetailResp);
        assertTrue(appDeletedDetailNode.get("code").asInt() != 0);
    }

    @Test
    void mediaUploadShouldReturnShortUrlAndSyncToMiniProduct() throws Exception {
        String token = loginToken();

        String appProductsResp = mockMvc.perform(
                        get("/api/v1/app/products")
                                .param("pageNo", "1")
                                .param("pageSize", "1")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode appProductsNode = objectMapper.readTree(appProductsResp);
        long productId = appProductsNode.get("data").get("list").get(0).get("id").asLong();

        String tinyPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+X2wYAAAAASUVORK5CYII=";
        String uploadResp = mockMvc.perform(
                        post("/api/v1/admin/files/upload")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"file_name\":\"mini-sync.png\",\"file_url\":\"data:image/png;base64," + tinyPng + "\",\"mime_type\":\"image/png\",\"file_size_kb\":1}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode uploadNode = objectMapper.readTree(uploadResp);
        assertEquals(0, uploadNode.get("code").asInt());
        String shortUrl = uploadNode.get("data").get("file").get("file_url").asText();
        assertTrue(shortUrl.contains("/api/v1/admin/files/"));
        assertTrue(shortUrl.endsWith("/content"));

        mockMvc.perform(
                        post("/api/v1/admin/products/spu/" + productId + "/media")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"media_type\":\"IMAGE\",\"media_url\":\"" + shortUrl + "\",\"sort_no\":9999}")
                ).andExpect(status().isOk());

        String appProductDetailResp = mockMvc.perform(
                        get("/api/v1/app/products/" + productId)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode productNode = objectMapper.readTree(appProductDetailResp);
        assertEquals(shortUrl, productNode.get("data").get("main_image_url").asText());
    }

    @Test
    void categoryCrudShouldSyncToMiniProgram() throws Exception {
        String token = loginToken();
        String categoryName = "sync-category";

        String createResp = mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"category_name\":\"" + categoryName + "\",\"sort_no\":88}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createNode = objectMapper.readTree(createResp);
        assertEquals(0, createNode.get("code").asInt());
        long categoryId = createNode.get("data").get("id").asLong();

        JsonNode appListAfterCreate = readAppCategoryList();
        JsonNode createdCategory = findCategoryById(appListAfterCreate, categoryId);
        assertTrue(createdCategory != null);
        assertEquals(categoryName, createdCategory.get("category_name").asText());
        assertEquals("ACTIVE", createdCategory.get("status_code").asText());

        String updatedName = "sync-category-updated";
        mockMvc.perform(
                        put("/api/v1/admin/categories/" + categoryId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"category_name\":\"" + updatedName + "\",\"sort_no\":66}")
                ).andExpect(status().isOk());

        JsonNode appListAfterUpdate = readAppCategoryList();
        JsonNode updatedCategory = findCategoryById(appListAfterUpdate, categoryId);
        assertTrue(updatedCategory != null);
        assertEquals(updatedName, updatedCategory.get("category_name").asText());
        assertEquals(66, updatedCategory.get("sort_no").asInt());

        mockMvc.perform(
                        put("/api/v1/admin/categories/" + categoryId + "/status")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status_code\":\"DISABLED\"}")
                ).andExpect(status().isOk());

        JsonNode appListAfterStatus = readAppCategoryList();
        JsonNode disabledCategory = findCategoryById(appListAfterStatus, categoryId);
        assertTrue(disabledCategory != null);
        assertEquals("DISABLED", disabledCategory.get("status_code").asText());

        mockMvc.perform(
                        delete("/api/v1/admin/categories/" + categoryId)
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk());

        JsonNode appListAfterDelete = readAppCategoryList();
        assertTrue(findCategoryById(appListAfterDelete, categoryId) == null);
    }

    @Test
    void recommendBannerShouldSyncToMiniHomeAndSupportLegacyImageUrlField() throws Exception {
        String token = loginToken();
        String tinyPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+X2wYAAAAASUVORK5CYII=";

        long fileId = 0L;
        long itemId = 0L;
        String shortUrl = "";
        try {
            String uploadResp = mockMvc.perform(
                            post("/api/v1/admin/files/upload")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"file_name\":\"banner-sync.png\",\"file_url\":\"data:image/png;base64," + tinyPng + "\",\"mime_type\":\"image/png\",\"file_size_kb\":1}")
                    ).andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode uploadNode = objectMapper.readTree(uploadResp);
            assertEquals(0, uploadNode.get("code").asInt());
            shortUrl = uploadNode.get("data").get("file").get("file_url").asText();
            fileId = uploadNode.get("data").get("file").get("id").asLong();
            assertTrue(shortUrl.contains("/api/v1/admin/files/"));
            assertTrue(shortUrl.endsWith("/content"));

            String createResp = mockMvc.perform(
                            post("/api/v1/admin/recommend-slots/201/items")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"spu_id\":0,\"product_name\":\"首页轮播图-兼容测试\",\"point_price\":0,\"sort_no\":998,\"status_code\":\"ENABLED\",\"start_at\":\"2026-01-01 00:00:00\",\"end_at\":\"2099-12-31 23:59:59\",\"image_url\":\"" + shortUrl + "\"}")
                    ).andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode createNode = objectMapper.readTree(createResp);
            assertEquals(0, createNode.get("code").asInt());
            itemId = createNode.get("data").get("id").asLong();
            assertEquals(shortUrl, createNode.get("data").get("banner_image_url").asText());

            String appHomeResp = mockMvc.perform(
                            get("/api/v1/app/home/recommends")
                    ).andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode appHomeNode = objectMapper.readTree(appHomeResp);
            JsonNode bannerList = appHomeNode.get("data").get("banner_list");
            boolean matched = false;
            for (JsonNode banner : bannerList) {
                if (shortUrl.equals(banner.asText())) {
                    matched = true;
                    break;
                }
            }
            assertTrue(matched);
        } finally {
            if (itemId > 0) {
                mockMvc.perform(
                        delete("/api/v1/admin/recommend-items/" + itemId)
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk());
            }
            if (fileId > 0) {
                mockMvc.perform(
                        delete("/api/v1/admin/files/" + fileId)
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk());
            }
        }
    }

    @Test
    void adminOrderShouldUseMiniProgramOrderDataWhenSameOrderIdExists() throws Exception {
        String appRemark = "app-order-remark-sync";
        String token = loginToken();

        String topUpResp = mockMvc.perform(
                        post("/api/v1/admin/users/1001/points/adjust")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"adjust_point\":50000,\"adjust_remark\":\"test-topup\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(0, objectMapper.readTree(topUpResp).get("code").asInt());

        String appToken = loginAppToken();
        long spuId = pickExchangeableSpuId();

        // 1) 新增地址
        String createAddressResp = mockMvc.perform(
                        post("/api/v1/app/addresses")
                                .header("Authorization", "Bearer " + appToken)
                                .header("X-Request-Id", "test-address-" + System.nanoTime())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"receiver_name\":\"测试用户\",\"receiver_phone\":\"13812345678\",\"province_name\":\"广东省\",\"city_name\":\"深圳市\",\"district_name\":\"南山区\",\"detail_address\":\"科技园 1 号\",\"is_default\":1,\"country_code\":\"CN\",\"status_code\":\"ACTIVE\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode addressNode = objectMapper.readTree(createAddressResp);
        assertEquals(0, addressNode.get("code").asInt());
        long addressId = addressNode.get("data").get("id").asLong();

        // 2) 小程序下单
        String submitResp = mockMvc.perform(
                        post("/api/v1/app/exchanges/orders")
                                .header("Authorization", "Bearer " + appToken)
                                .header("X-Request-Id", "test-order-" + System.nanoTime())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"spu_id\":" + spuId + ",\"quantity\":1,\"address_id\":" + addressId + ",\"source_scene_code\":\"WX_MINI_PROGRAM\",\"user_remark\":\"" + appRemark + "\"}")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode submitNode = objectMapper.readTree(submitResp);
        assertEquals(0, submitNode.get("code").asInt());
        String orderNo = submitNode.get("data").get("order_no").asText();

        // 3) 触发管理端同步，并校验能看到小程序订单（修复ID冲突导致看不到订单的问题）
        String listResp = mockMvc.perform(
                        get("/api/v1/admin/orders")
                                .header("Authorization", "Bearer " + token)
                                .param("keyword", orderNo)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode list = objectMapper.readTree(listResp).get("data").get("list");
        assertTrue(list.size() > 0);
        assertEquals(appRemark, list.get(0).get("remark").asText());
    }

    private JsonNode readAppCategoryList() throws Exception {
        String resp = mockMvc.perform(
                        get("/api/v1/app/categories")
                                .param("pageNo", "1")
                                .param("pageSize", "500")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resp).get("data").get("list");
    }

    private JsonNode findCategoryById(JsonNode list, long categoryId) {
        for (JsonNode item : list) {
            if (item.get("id").asLong() == categoryId) {
                return item;
            }
        }
        return null;
    }
}
