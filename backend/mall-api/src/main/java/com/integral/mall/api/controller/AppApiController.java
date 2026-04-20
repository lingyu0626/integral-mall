package com.integral.mall.api.controller;

import com.integral.mall.api.common.ApiResponse;
import com.integral.mall.api.service.AppCatalogService;
import com.integral.mall.api.service.JwtTokenService;
import com.integral.mall.api.service.RedisCacheService;
import com.integral.mall.api.service.WechatMiniAuthService;
import com.integral.mall.api.store.InMemoryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AppApiController {
    private static final Pattern WECHAT_DEFAULT_NICK = Pattern.compile("^微信用户\\d*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADMIN_FILE_CONTENT_URL_PATTERN =
            Pattern.compile("^https?://[^/]+(/api/v1/admin/files/\\d+/content)(?:[?#].*)?$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private InMemoryData store;
    @Autowired
    private WechatMiniAuthService wechatMiniAuthService;
    @Autowired(required = false)
    private AdminBusinessController adminBusinessController;
    @Autowired(required = false)
    private RedisCacheService redisCacheService;
    @Autowired(required = false)
    private JwtTokenService jwtTokenService;
    @Autowired(required = false)
    private AppCatalogService appCatalogService;
    @Value("${mall.security.allow-legacy-token:false}")
    private boolean allowLegacyToken;

    // =========================
    // auth & user
    // =========================
    @PostMapping("/auth/wx-login")
    public ApiResponse<Map<String, Object>> wxLogin(@RequestBody(required = false) Map<String, Object> payload) {
        String code = payload == null ? "" : String.valueOf(payload.getOrDefault("code", ""));
        if (!StringUtils.hasText(code)) {
            return ApiResponse.fail(4001, "code 不能为空");
        }
        WechatMiniAuthService.WxSession wxSession;
        try {
            wxSession = wechatMiniAuthService.code2Session(code);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "微信登录失败" : ex.getMessage();
            return ApiResponse.fail(4002, message);
        }

        Map<String, Object> user = store.findOrCreateWechatUser(wxSession.getOpenId(), wxSession.getUnionId());
        if (user == null) {
            return ApiResponse.fail(5001, "登录失败：用户信息初始化异常");
        }
        syncAdminSnapshotIfPossible();
        Long userId = InMemoryData.toLong(user.get("id"));
        String phoneMasked = String.valueOf(user.getOrDefault("phone_masked", ""));
        boolean needBindPhone = !StringUtils.hasText(phoneMasked);
        Map<String, Object> data = buildLoginData(userId, needBindPhone, true);
        if (!StringUtils.hasText(String.valueOf(data.getOrDefault("access_token", "")))) {
            return ApiResponse.fail(5002, "JWT 服务不可用，请检查 mall.jwt.secret 配置");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/auth/bind-phone")
    public ApiResponse<Map<String, Object>> bindPhone(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        Long userId = requireUserId(request);
        ensureUserActive(userId);
        String phoneCode = String.valueOf(payload.getOrDefault("phone_code", payload.getOrDefault("code", "")));
        if (!StringUtils.hasText(phoneCode)) {
            return ApiResponse.fail(4002, "手机号授权 code 不能为空");
        }
        String phone;
        try {
            phone = wechatMiniAuthService.resolvePhoneByCode(phoneCode);
        } catch (Exception ex) {
            return ApiResponse.fail(4003, ex.getMessage());
        }
        store.bindWechatUserPhone(userId, phone);
        Map<String, Object> user = store.getUser(userId);
        normalizeNickNameAfterPhoneBind(user);
        syncAdminSnapshotIfPossible();
        return ApiResponse.ok(user);
    }

    @PostMapping("/auth/refresh-token")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody(required = false) Map<String, Object> payload) {
        String refreshToken = payload == null ? "" : String.valueOf(payload.getOrDefault("refresh_token", ""));
        Long userId = jwtTokenService == null ? null : jwtTokenService.parseAppRefreshUserId(refreshToken);
        if (userId == null && allowLegacyToken) {
            userId = store.getUserIdByRefresh(refreshToken);
        }
        if (userId == null) {
            return ApiResponse.fail(4010, "refresh_token 无效");
        }
        String accessToken = buildAccessToken(userId);
        String newRefreshToken = buildRefreshToken(userId);
        if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(newRefreshToken)) {
            return ApiResponse.fail(5002, "JWT 服务不可用，请检查 mall.jwt.secret 配置");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("refresh_token", newRefreshToken);
        data.put("user_id", userId);
        return ApiResponse.ok(data);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (StringUtils.hasText(token)) {
            store.removeToken(token);
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/users/me")
    public ApiResponse<Map<String, Object>> getMe(HttpServletRequest request) {
        return ApiResponse.ok(store.getUser(resolveUserIdOrDefault(request)));
    }

    @PutMapping("/users/me")
    public ApiResponse<Map<String, Object>> updateMe(HttpServletRequest request,
                                                     @RequestBody(required = false) Map<String, Object> payload) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        Map<String, Object> user = store.getUser(userId);
        if (user == null) {
            return ApiResponse.fail(4041, "用户不存在");
        }
        if (payload != null) {
            if (payload.containsKey("nick_name")) {
                String nickName = String.valueOf(payload.get("nick_name"));
                if (StringUtils.hasText(nickName)) {
                    user.put("nick_name", normalizeNickNameInput(nickName, user));
                }
            }
            if (payload.containsKey("avatar_url")) {
                String avatarUrl = String.valueOf(payload.get("avatar_url"));
                if (StringUtils.hasText(avatarUrl)) {
                    user.put("avatar_url", avatarUrl.trim());
                }
            }
        }
        syncAdminSnapshotIfPossible();
        return ApiResponse.ok(user);
    }

    @GetMapping("/users/me/summary")
    public ApiResponse<Map<String, Object>> getMeSummary(HttpServletRequest request) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> user = store.getUser(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("phone_masked", store.getUser(userId).get("phone_masked"));
        data.put("point_balance", store.getPointAccount(userId).get("point_balance"));
        data.put("total_consume_amount", user == null ? 0D : user.getOrDefault("total_consume_amount", 0D));
        data.put("profit_amount", user == null ? 0D : user.getOrDefault("profit_amount", 0D));
        data.put("pending_audit_count", store.countOrdersByStatus(userId, "PENDING_AUDIT"));
        data.put("pending_ship_count", store.countOrdersByStatus(userId, "PENDING_SHIP"));
        data.put("shipped_count", store.countOrdersByStatus(userId, "SHIPPED"));
        data.put("finished_count", store.countOrdersByStatus(userId, "FINISHED"));
        return ApiResponse.ok(data);
    }

    // =========================
    // home/category/product
    // =========================
    @GetMapping("/home/recommends")
    public ApiResponse<Map<String, Object>> homeRecommends(HttpServletRequest request) {
        Long userId = resolveUserIdIfPresent(request);
        List<Map<String, Object>> list = cacheOrLoad(
                "mall:app:home:recommend:list:v1",
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {},
                Duration.ofSeconds(30),
                this::resolveHomeRecommendProducts
        );
        List<String> banners = cacheOrLoad(
                "mall:app:home:recommend:banners:v1",
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {},
                Duration.ofSeconds(30),
                () -> new ArrayList<>(store.getHomeBanners())
        );
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId == null ? 0L : userId);
        if (userId != null) {
            data.put("point_balance", store.getPointAccount(userId).get("point_balance"));
            data.put("phone_masked", store.getUser(userId).get("phone_masked"));
        } else {
            data.put("point_balance", 0);
            data.put("phone_masked", "");
        }
        data.put("banner_list", banners);
        data.put("list", list);
        return ApiResponse.ok(data);
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, Object>> getCategories(@RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "100") int pageSize) {
        String cacheKey = String.format("mall:app:catalog:categories:p%d:s%d", Math.max(pageNo, 1), Math.max(pageSize, 1));
        Map<String, Object> data = cacheOrLoad(
                cacheKey,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {},
                Duration.ofSeconds(60),
                () -> {
                    List<Map<String, Object>> all = listCategoriesFromSource();
                    all.sort(Comparator.comparing(item -> InMemoryData.toInt(item.get("sort_no"))));
                    return store.pageResult(all, pageNo, pageSize);
                }
        );
        return ApiResponse.ok(data);
    }

    @GetMapping("/categories/{categoryId}/products")
    public ApiResponse<Map<String, Object>> getCategoryProducts(@PathVariable Long categoryId,
                                                                @RequestParam(defaultValue = "1") int pageNo,
                                                                @RequestParam(defaultValue = "20") int pageSize,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false, name = "sort_by") String sortBy) {
        String safeSort = String.valueOf(sortBy == null ? "" : sortBy).trim().toLowerCase(Locale.ROOT);
        String safeKeyword = String.valueOf(keyword == null ? "" : keyword).trim();
        String cacheKey = String.format(
                "mall:app:catalog:category:%d:p%d:s%d:sort:%s:kw:%s",
                categoryId == null ? 0L : categoryId,
                Math.max(pageNo, 1),
                Math.max(pageSize, 1),
                safeSort,
                safeKeyword
        );
        Map<String, Object> data = cacheOrLoad(
                cacheKey,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {},
                Duration.ofSeconds(45),
                () -> {
                    List<Map<String, Object>> all = listProductsFromSource().stream()
                            .filter(item -> matchesProductCategory(item, categoryId))
                            .filter(item -> !StringUtils.hasText(safeKeyword) || String.valueOf(item.get("product_name")).contains(safeKeyword))
                            .collect(Collectors.toList());
                    if ("point_asc".equals(safeSort)) {
                        all.sort(Comparator.comparingLong(item -> InMemoryData.toLong(item.getOrDefault("point_price", 0))));
                    } else if ("point_desc".equals(safeSort)) {
                        all.sort((a, b) -> Long.compare(
                                InMemoryData.toLong(b.getOrDefault("point_price", 0)),
                                InMemoryData.toLong(a.getOrDefault("point_price", 0))
                        ));
                    }
                    return store.pageResult(all, pageNo, pageSize);
                }
        );
        return ApiResponse.ok(data);
    }

    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> searchProducts(@RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize,
                                                           @RequestParam(required = false) String keyword) {
        String safeKeyword = String.valueOf(keyword == null ? "" : keyword).trim();
        String cacheKey = String.format(
                "mall:app:catalog:products:p%d:s%d:kw:%s",
                Math.max(pageNo, 1),
                Math.max(pageSize, 1),
                safeKeyword
        );
        Map<String, Object> data = cacheOrLoad(
                cacheKey,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {},
                Duration.ofSeconds(45),
                () -> {
                    List<Map<String, Object>> all = listProductsFromSource().stream()
                            .filter(item -> !StringUtils.hasText(safeKeyword) || String.valueOf(item.get("product_name")).contains(safeKeyword))
                            .collect(Collectors.toList());
                    return store.pageResult(all, pageNo, pageSize);
                }
        );
        return ApiResponse.ok(data);
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<Map<String, Object>> getProductDetail(@PathVariable Long productId) {
        String cacheKey = String.format("mall:app:catalog:product:%d", productId == null ? 0L : productId);
        Map<String, Object> cachedProduct = cacheOrLoad(
                cacheKey,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {},
                Duration.ofSeconds(60),
                () -> getProductFromSource(productId)
        );
        if (cachedProduct == null) {
            return ApiResponse.fail(4041, "商品不存在");
        }
        Map<String, Object> product = enrichProductDetailWithMedias(productId, cachedProduct);
        return ApiResponse.ok(product);
    }

    @GetMapping("/products/{productId}/exchange-preview")
    public ApiResponse<Map<String, Object>> getExchangePreview(HttpServletRequest request,
                                                               @PathVariable Long productId,
                                                               @RequestParam(required = false, name = "sku_id") Long skuId) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> product = store.getProduct(productId);
        if (product == null) {
            return ApiResponse.fail(4041, "商品不存在");
        }
        long pointPrice = InMemoryData.toLong(product.get("point_price"));
        long balance = InMemoryData.toLong(store.getPointAccount(userId).get("point_balance"));
        long stock = InMemoryData.toLong(product.get("stock_available"));
        if (skuId != null && skuId > 0) {
            Object rawSkuList = product.get("sku_list");
            if (rawSkuList instanceof List<?>) {
                for (Object rawSku : (List<?>) rawSkuList) {
                    if (!(rawSku instanceof Map<?, ?>)) continue;
                    Map<?, ?> sku = (Map<?, ?>) rawSku;
                    Long currentSkuId = InMemoryData.toLong(sku.get("sku_id"));
                    if (!Objects.equals(currentSkuId, skuId)) continue;
                    pointPrice = InMemoryData.toLong(sku.get("point_price"));
                    stock = InMemoryData.toLong(sku.get("stock_available"));
                    break;
                }
            }
        }
        String typeCode = String.valueOf(product.getOrDefault("product_type_code", "VIRTUAL"));
        boolean canExchange = balance >= pointPrice && stock > 0;

        Map<String, Object> data = new HashMap<>();
        data.put("can_exchange", canExchange);
        data.put("reason", canExchange ? "满足兑换条件" : (balance < pointPrice ? "积分不足" : "库存不足"));
        data.put("point_balance", balance);
        data.put("required_point", pointPrice);
        data.put("require_address", "PHYSICAL".equalsIgnoreCase(typeCode));
        return ApiResponse.ok(data);
    }

    // =========================
    // addresses
    // =========================
    @GetMapping("/addresses")
    public ApiResponse<Map<String, Object>> getAddresses(HttpServletRequest request,
                                                         @RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = resolveUserIdOrDefault(request);
        List<Map<String, Object>> list = new ArrayList<>(store.userAddressMap(userId).values());
        return ApiResponse.ok(store.pageResult(list, pageNo, pageSize));
    }

    @PostMapping("/addresses")
    public ApiResponse<Map<String, Object>> createAddress(HttpServletRequest request,
                                                          @RequestBody Map<String, Object> payload) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        return ApiResponse.ok(store.createAddress(userId, payload));
    }

    @GetMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> getAddressDetail(HttpServletRequest request, @PathVariable Long addressId) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> item = store.userAddressMap(userId).get(addressId);
        if (item == null) return ApiResponse.fail(4042, "地址不存在");
        return ApiResponse.ok(item);
    }

    @PutMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> updateAddress(HttpServletRequest request,
                                                          @PathVariable Long addressId,
                                                          @RequestBody Map<String, Object> payload) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        Map<String, Object> item = store.updateAddress(userId, addressId, payload);
        if (item == null) return ApiResponse.fail(4042, "地址不存在");
        return ApiResponse.ok(item);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> deleteAddress(HttpServletRequest request, @PathVariable Long addressId) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        boolean success = store.removeAddress(userId, addressId);
        if (!success) return ApiResponse.fail(4042, "地址不存在");
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/addresses/{addressId}/default")
    public ApiResponse<Map<String, Object>> setDefaultAddress(HttpServletRequest request, @PathVariable Long addressId) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        boolean success = store.setDefaultAddress(userId, addressId);
        if (!success) return ApiResponse.fail(4042, "地址不存在");
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // exchange & orders
    // =========================
    @PostMapping("/exchanges/orders")
    public ApiResponse<Map<String, Object>> submitExchangeOrder(HttpServletRequest request,
                                                                @RequestBody Map<String, Object> payload) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        String requestId = request.getHeader("X-Request-Id");
        Long spuId = InMemoryData.toLong(payload.get("spu_id"));
        Long skuId = payload.get("sku_id") == null ? null : InMemoryData.toLong(payload.get("sku_id"));
        Long addressId = payload.get("address_id") == null ? null : InMemoryData.toLong(payload.get("address_id"));
        String userRemark = payload.get("user_remark") == null ? "" : String.valueOf(payload.get("user_remark"));
        Map<String, Object> data = store.createOrder(userId, spuId, skuId, addressId, requestId, userRemark);
        if (data == null) {
            return ApiResponse.fail(4003, "兑换失败：积分不足/库存不足/缺少地址");
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> getOrders(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String order_status_code) {
        Long userId = resolveUserIdOrDefault(request);
        List<Map<String, Object>> list = store.listOrdersByUser(userId, order_status_code);
        return ApiResponse.ok(store.pageResult(list, pageNo, pageSize));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> getOrderDetail(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> order = store.getOrder(orderId);
        if (order == null || !Objects.equals(userId, InMemoryData.toLong(order.get("user_id")))) {
            return ApiResponse.fail(4043, "订单不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", store.getOrderItems(orderId));
        data.put("address_snapshot", store.getOrderAddressSnapshot(orderId));
        return ApiResponse.ok(data);
    }

    @GetMapping("/orders/{orderId}/flows")
    public ApiResponse<Map<String, Object>> getOrderFlows(HttpServletRequest request,
                                                          @PathVariable Long orderId,
                                                          @RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "50") int pageSize) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> order = store.getOrder(orderId);
        if (order == null || !Objects.equals(userId, InMemoryData.toLong(order.get("user_id")))) {
            return ApiResponse.fail(4043, "订单不存在");
        }
        return ApiResponse.ok(store.pageResult(store.getOrderFlows(orderId), pageNo, pageSize));
    }

    @GetMapping("/orders/{orderId}/delivery")
    public ApiResponse<Map<String, Object>> getOrderDelivery(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> order = store.getOrder(orderId);
        if (order == null || !Objects.equals(userId, InMemoryData.toLong(order.get("user_id")))) {
            return ApiResponse.fail(4043, "订单不存在");
        }
        Map<String, Object> data = store.getOrderDelivery(orderId);
        return ApiResponse.ok(data == null ? new HashMap<>() : data);
    }

    @GetMapping("/orders/{orderId}/logistics-traces")
    public ApiResponse<Map<String, Object>> getOrderLogisticsTraces(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> order = store.getOrder(orderId);
        if (order == null || !Objects.equals(userId, InMemoryData.toLong(order.get("user_id")))) {
            return ApiResponse.fail(4043, "订单不存在");
        }
        if (adminBusinessController != null) {
            return ApiResponse.ok(adminBusinessController.queryOrderLogisticsForApp(orderId));
        }

        Map<String, Object> delivery = store.getOrderDelivery(orderId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("company_name", delivery == null ? "" : String.valueOf(delivery.getOrDefault("company_name", delivery.getOrDefault("express_company", ""))));
        data.put("express_no", delivery == null ? "" : String.valueOf(delivery.getOrDefault("tracking_no", delivery.getOrDefault("express_no", ""))));
        data.put("shipper_code", delivery == null ? "" : String.valueOf(delivery.getOrDefault("shipper_code", "")));
        data.put("shipper_phone", delivery == null ? "" : String.valueOf(delivery.getOrDefault("shipper_phone", "")));
        data.put("delivery_status_code", delivery == null ? "" : String.valueOf(delivery.getOrDefault("delivery_status_code", "")));
        data.put("delivery_status_text", delivery == null ? "" : String.valueOf(delivery.getOrDefault("delivery_status_text", "")));
        data.put("latest_trace_time", delivery == null ? "" : String.valueOf(delivery.getOrDefault("latest_trace_time", "")));
        data.put("latest_trace_station", delivery == null ? "" : String.valueOf(delivery.getOrDefault("latest_trace_station", "")));
        data.put("ship_at", delivery == null ? "" : String.valueOf(delivery.getOrDefault("ship_at", "")));
        data.put("signed_at", delivery == null ? "" : String.valueOf(delivery.getOrDefault("signed_at", "")));
        Map<String, Object> address = store.getOrderAddressSnapshot(orderId);
        data.put("receiver_address", address == null ? "" : String.valueOf(address.getOrDefault("detail_address", "")));
        data.put("provider_tip", "本数据由快递公司提供");
        data.put("trace_source", "FALLBACK");
        List<Map<String, Object>> traces = new ArrayList<>();
        if (delivery != null) {
            String latestTime = String.valueOf(delivery.getOrDefault("latest_trace_time", ""));
            String latestStation = String.valueOf(delivery.getOrDefault("latest_trace_station", ""));
            if (StringUtils.hasText(latestTime) || StringUtils.hasText(latestStation)) {
                Map<String, Object> one = new LinkedHashMap<>();
                one.put("accept_time", latestTime);
                one.put("accept_station", latestStation);
                one.put("status", String.valueOf(delivery.getOrDefault("delivery_status_text", "")));
                traces.add(one);
            }
        }
        data.put("traces", traces);
        return ApiResponse.ok(data);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<Map<String, Object>> cancelOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        boolean success = store.cancelOrder(userId, orderId);
        if (!success) {
            return ApiResponse.fail(4004, "当前订单状态不允许取消");
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/orders/{orderId}/reject-decision")
    public ApiResponse<Map<String, Object>> decideRejectedOrder(HttpServletRequest request,
                                                                @PathVariable Long orderId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        String decision = payload == null ? "" : String.valueOf(payload.getOrDefault("decision", ""));
        String note = payload == null ? "" : String.valueOf(payload.getOrDefault("note", ""));
        boolean success = store.reviewRejectedOrder(userId, orderId, decision, note);
        if (!success) {
            return ApiResponse.fail(4006, "当前订单不支持该操作");
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/orders/status-counts")
    public ApiResponse<Map<String, Object>> getOrderStatusCounts(HttpServletRequest request) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> data = new HashMap<>();
        data.put("total_count", store.countOrders(userId));
        data.put("pending_audit_count", store.countOrdersByStatus(userId, "PENDING_AUDIT"));
        data.put("pending_ship_count", store.countOrdersByStatus(userId, "PENDING_SHIP"));
        data.put("shipped_count", store.countOrdersByStatus(userId, "SHIPPED"));
        data.put("finished_count", store.countOrdersByStatus(userId, "FINISHED"));
        return ApiResponse.ok(data);
    }

    // =========================
    // points
    // =========================
    @GetMapping("/points/account")
    public ApiResponse<Map<String, Object>> getPointAccount(HttpServletRequest request) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> account = new LinkedHashMap<>(store.getPointAccount(userId));
        Map<String, Object> user = store.getUser(userId);
        account.put("total_consume_amount", user == null ? 0D : user.getOrDefault("total_consume_amount", 0D));
        account.put("profit_amount", user == null ? 0D : user.getOrDefault("profit_amount", 0D));
        return ApiResponse.ok(account);
    }

    @GetMapping("/points/ledger")
    public ApiResponse<Map<String, Object>> getPointLedger(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = resolveUserIdOrDefault(request);
        List<Map<String, Object>> source = store.getPointLedgers(userId);
        return ApiResponse.ok(store.pageResult(enrichPointLedgersForDisplay(userId, source), pageNo, pageSize));
    }

    @GetMapping("/points/ledger/{ledgerId}")
    public ApiResponse<Map<String, Object>> getPointLedgerDetail(HttpServletRequest request, @PathVariable Long ledgerId) {
        Long userId = resolveUserIdOrDefault(request);
        return enrichPointLedgersForDisplay(userId, store.getPointLedgers(userId)).stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("id")), ledgerId))
                .findFirst()
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail(4044, "积分流水不存在"));
    }

    // =========================
    // backpack
    // =========================
    @GetMapping("/backpack/assets")
    public ApiResponse<Map<String, Object>> getBackpackAssets(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = resolveUserIdOrDefault(request);
        return ApiResponse.ok(store.pageResult(store.listAssetsByUser(userId), pageNo, pageSize));
    }

    @GetMapping("/backpack/assets/{assetId}")
    public ApiResponse<Map<String, Object>> getBackpackAssetDetail(HttpServletRequest request, @PathVariable Long assetId) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> data = store.getAsset(assetId);
        if (data == null || !Objects.equals(userId, InMemoryData.toLong(data.get("user_id")))) {
            return ApiResponse.fail(4045, "资产不存在");
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/backpack/assets/{assetId}/flows")
    public ApiResponse<Map<String, Object>> getBackpackAssetFlows(HttpServletRequest request,
                                                                  @PathVariable Long assetId,
                                                                  @RequestParam(defaultValue = "1") int pageNo,
                                                                  @RequestParam(defaultValue = "50") int pageSize) {
        Long userId = resolveUserIdOrDefault(request);
        Map<String, Object> asset = store.getAsset(assetId);
        if (asset == null || !Objects.equals(userId, InMemoryData.toLong(asset.get("user_id")))) {
            return ApiResponse.fail(4045, "资产不存在");
        }
        return ApiResponse.ok(store.pageResult(store.getAssetFlows(assetId), pageNo, pageSize));
    }

    @PostMapping("/backpack/assets/{assetId}/use")
    public ApiResponse<Map<String, Object>> useBackpackAsset(HttpServletRequest request, @PathVariable Long assetId) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        boolean success = store.useAsset(userId, assetId);
        if (!success) return ApiResponse.fail(4005, "资产不可使用");
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // group/service/dict/config
    // =========================
    @GetMapping("/group-resources")
    public ApiResponse<Map<String, Object>> getGroupResources(@RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(store.pageResult(store.listGroupResources(), pageNo, pageSize));
    }

    @GetMapping("/group-resources/{resourceId}")
    public ApiResponse<Map<String, Object>> getGroupResourceDetail(@PathVariable Long resourceId) {
        Map<String, Object> data = store.getGroupResource(resourceId);
        if (data == null) return ApiResponse.fail(4046, "资源不存在");
        return ApiResponse.ok(data);
    }

    @GetMapping("/customer-service/contact")
    public ApiResponse<Map<String, Object>> getCustomerServiceContact() {
        return ApiResponse.ok(store.getCustomerServiceContact());
    }

    @GetMapping("/dict/{dictTypeCode}/items")
    public ApiResponse<Map<String, Object>> getDictItems(@PathVariable String dictTypeCode,
                                                         @RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.ok(store.pageResult(store.getDictItems(dictTypeCode), pageNo, pageSize));
    }

    @GetMapping("/system-configs/public")
    public ApiResponse<Map<String, Object>> getPublicConfigs(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "100") int pageSize) {
        if (adminBusinessController != null) {
            List<Map<String, Object>> source = adminBusinessController.snapshotSystemConfigs().stream()
                    .filter(item -> "ENABLED".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ENABLED"))))
                    .sorted((a, b) -> String.valueOf(b.getOrDefault("updated_at", "")).compareTo(String.valueOf(a.getOrDefault("updated_at", ""))))
                    .map(item -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", item.get("id"));
                        row.put("config_key", String.valueOf(item.getOrDefault("config_key", "")));
                        row.put("config_name", String.valueOf(item.getOrDefault("config_name", "")));
                        row.put("config_value", String.valueOf(item.getOrDefault("config_value", "")));
                        row.put("group_code", String.valueOf(item.getOrDefault("group_code", "")));
                        row.put("updated_at", String.valueOf(item.getOrDefault("updated_at", "")));
                        return row;
                    })
                    .collect(Collectors.toList());
            return ApiResponse.ok(store.pageResult(source, pageNo, pageSize));
        }
        return ApiResponse.ok(store.pageResult(store.getPublicConfigs(), pageNo, pageSize));
    }

    @PostMapping("/wish-demands")
    public ApiResponse<Map<String, Object>> createWishDemand(HttpServletRequest request,
                                                             @RequestBody(required = false) Map<String, Object> payload) {
        Long userId = resolveUserIdOrDefault(request);
        ensureUserActive(userId);
        if (adminBusinessController == null) {
            return ApiResponse.fail(4001, "留言功能暂不可用");
        }
        Map<String, Object> created = adminBusinessController.createWishDemandFromApp(userId, payload);
        if (created == null) {
            return ApiResponse.fail(4002, "请至少填写商品名称或留言内容");
        }
        return ApiResponse.ok(created);
    }

    @GetMapping("/wish-demands")
    public ApiResponse<Map<String, Object>> listWishDemands(HttpServletRequest request,
                                                            @RequestParam(defaultValue = "1") int pageNo,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = resolveUserIdOrDefault(request);
        if (adminBusinessController == null) {
            return ApiResponse.ok(store.pageResult(new ArrayList<>(), pageNo, pageSize));
        }
        return ApiResponse.ok(adminBusinessController.listWishDemandsForApp(userId, pageNo, pageSize));
    }

    // =========================
    // helper
    // =========================
    private List<Map<String, Object>> listCategoriesFromSource() {
        if (appCatalogService != null) {
            return appCatalogService.listCategories();
        }
        return new ArrayList<>(store.listCategories());
    }

    private List<Map<String, Object>> listProductsFromSource() {
        List<Map<String, Object>> source;
        if (appCatalogService != null) {
            source = appCatalogService.listProducts();
        } else {
            source = store.listProducts();
        }
        return enrichProductsWithAdminCategoryIds(source);
    }

    private Map<String, Object> getProductFromSource(Long productId) {
        if (appCatalogService != null) {
            Map<String, Object> product = appCatalogService.getProduct(productId);
            if (product != null) return product;
        }
        return store.getProduct(productId);
    }

    private List<Map<String, Object>> enrichPointLedgersForDisplay(Long userId, List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : source) {
            result.add(item == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item));
        }
        if (userId == null || userId <= 0) return result;

        Map<String, String> orderNameByTimeAndAmount = new LinkedHashMap<>();
        Map<Long, List<String>> orderNamesByAmount = new LinkedHashMap<>();
        for (Map<String, Object> order : store.listOrdersByUser(userId, null)) {
            if (order == null) continue;
            String orderName = resolveOrderLedgerName(order);
            if (!StringUtils.hasText(orderName)) continue;
            long amount = Math.max(0L, InMemoryData.toLong(order.getOrDefault("total_point_amount", 0)));
            String submitAt = String.valueOf(order.getOrDefault("submit_at", ""));
            if (amount <= 0L) continue;
            if (StringUtils.hasText(submitAt)) {
                orderNameByTimeAndAmount.putIfAbsent(buildLedgerOrderMatchKey(submitAt, amount), orderName);
            }
            orderNamesByAmount.computeIfAbsent(amount, key -> new ArrayList<>()).add(orderName);
        }

        Map<Long, Integer> amountCursor = new HashMap<>();
        for (Map<String, Object> row : result) {
            if (row == null) continue;
            String bizTypeCode = String.valueOf(row.getOrDefault("biz_type_code", "")).trim().toUpperCase(Locale.ROOT);
            if (!"EXCHANGE_ORDER".equals(bizTypeCode)) continue;

            String remark = String.valueOf(row.getOrDefault("remark", row.getOrDefault("note", ""))).trim();
            String exchangeName = String.valueOf(row.getOrDefault("exchange_name", "")).trim();
            if (!StringUtils.hasText(exchangeName)) {
                exchangeName = extractExchangeNameFromRemark(remark);
            }
            if (!StringUtils.hasText(exchangeName)) {
                long amount = Math.abs(InMemoryData.toLong(row.getOrDefault("change_amount", 0)));
                String occurredAt = String.valueOf(row.getOrDefault("occurred_at", "")).trim();
                if (StringUtils.hasText(occurredAt) && amount > 0L) {
                    exchangeName = orderNameByTimeAndAmount.getOrDefault(buildLedgerOrderMatchKey(occurredAt, amount), "");
                }
                if (!StringUtils.hasText(exchangeName) && amount > 0L) {
                    List<String> candidates = orderNamesByAmount.getOrDefault(amount, new ArrayList<>());
                    int index = amountCursor.getOrDefault(amount, 0);
                    if (index < candidates.size()) {
                        exchangeName = candidates.get(index);
                        amountCursor.put(amount, index + 1);
                    }
                }
            }

            if (StringUtils.hasText(exchangeName)) {
                row.put("exchange_name", exchangeName);
                if (!StringUtils.hasText(remark)) {
                    row.put("remark", "兑换扣减：" + exchangeName);
                }
            } else if (!StringUtils.hasText(remark)) {
                row.put("remark", "兑换扣减");
            }
        }
        return result;
    }

    private String resolveOrderLedgerName(Map<String, Object> order) {
        if (order == null) return "";
        String productName = String.valueOf(order.getOrDefault("product_name_snapshot", "")).trim();
        String skuName = "";
        int quantity = Math.max(1, InMemoryData.toInt(order.getOrDefault("total_item_count", 1)));
        Long orderId = InMemoryData.toLong(order.getOrDefault("id", 0));
        if (orderId != null && orderId > 0) {
            List<Map<String, Object>> items = store.getOrderItems(orderId);
            if (!items.isEmpty()) {
                Map<String, Object> first = items.get(0);
                if (!StringUtils.hasText(productName)) {
                    productName = String.valueOf(first.getOrDefault("product_name_snapshot", first.getOrDefault("spu_name", ""))).trim();
                }
                skuName = String.valueOf(first.getOrDefault("sku_name_snapshot", first.getOrDefault("sku_name", ""))).trim();
                quantity = Math.max(1, InMemoryData.toInt(first.getOrDefault("quantity", quantity)));
            }
        }
        if (!StringUtils.hasText(productName)) return "";
        StringBuilder builder = new StringBuilder(productName);
        if (StringUtils.hasText(skuName)) {
            builder.append(" ").append(skuName);
        }
        if (quantity > 1) {
            builder.append(" x").append(quantity);
        }
        return builder.toString();
    }

    private String buildLedgerOrderMatchKey(String timeText, long amount) {
        return String.valueOf(timeText == null ? "" : timeText).trim() + "#" + Math.max(0L, amount);
    }

    private String extractExchangeNameFromRemark(String remark) {
        if (!StringUtils.hasText(remark)) return "";
        String text = String.valueOf(remark).trim();
        if (!text.startsWith("兑换扣减")) return text;
        int idxCn = text.indexOf('：');
        int idxEn = text.indexOf(':');
        int idx = idxCn >= 0 ? idxCn : idxEn;
        if (idx >= 0 && idx < text.length() - 1) {
            return text.substring(idx + 1).trim();
        }
        return "";
    }

    private List<Map<String, Object>> enrichProductsWithAdminCategoryIds(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) return new ArrayList<>();
        if (adminBusinessController == null) return source;

        Map<Long, Map<String, Object>> adminSpuMap = adminBusinessController.snapshotSpus().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        row -> InMemoryData.toLong(row.getOrDefault("id", 0)),
                        row -> row,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        if (adminSpuMap.isEmpty()) return source;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : source) {
            Map<String, Object> mapped = item == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item);
            Long spuId = InMemoryData.toLong(mapped.getOrDefault("id", mapped.getOrDefault("spu_id", 0)));
            Map<String, Object> adminSpu = adminSpuMap.get(spuId);
            if (adminSpu != null) {
                List<Long> categoryIds = resolveCategoryIdList(
                        adminSpu.get("category_ids"),
                        InMemoryData.toLong(adminSpu.getOrDefault("category_id", mapped.getOrDefault("category_id", 0)))
                );
                if (!categoryIds.isEmpty()) {
                    mapped.put("category_ids", categoryIds);
                    mapped.put("category_id", categoryIds.get(0));
                }
                String categoryName = String.valueOf(adminSpu.getOrDefault("category_name", "")).trim();
                if (StringUtils.hasText(categoryName)) {
                    mapped.put("category_name", categoryName);
                }
            }
            result.add(mapped);
        }
        return result;
    }

    private List<Long> resolveCategoryIdList(Object rawCategoryIds, Long fallback) {
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        if (rawCategoryIds instanceof Collection<?>) {
            for (Object one : (Collection<?>) rawCategoryIds) {
                Long id = InMemoryData.toLong(one);
                if (id != null && id > 0) set.add(id);
            }
        } else if (rawCategoryIds instanceof String) {
            String[] parts = String.valueOf(rawCategoryIds).split("[,，\\s]+");
            for (String part : parts) {
                if (!StringUtils.hasText(part)) continue;
                try {
                    long id = Long.parseLong(part.trim());
                    if (id > 0) set.add(id);
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
        if ((set.isEmpty()) && fallback != null && fallback > 0) {
            set.add(fallback);
        }
        return new ArrayList<>(set);
    }

    private Map<String, Object> enrichProductDetailWithMedias(Long productId, Map<String, Object> source) {
        Map<String, Object> mapped = source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
        LinkedHashMap<String, String> imageUrlMap = new LinkedHashMap<>();
        appendImageUrlsFromRaw(mapped.get("image_urls"), imageUrlMap);
        appendImageUrl(imageUrlMap, mapped.get("main_image_url"));
        appendImageUrl(imageUrlMap, mapped.get("image_url"));

        if (adminBusinessController != null && productId != null && productId > 0) {
            List<Map<String, Object>> mediaRows = adminBusinessController.snapshotSpuMedias(productId);
            if (!mediaRows.isEmpty()) {
                List<Map<String, Object>> mediaList = new ArrayList<>();
                for (Map<String, Object> media : mediaRows) {
                    if (media == null) continue;
                    String mediaType = String.valueOf(media.getOrDefault("media_type", "IMAGE")).trim().toUpperCase(Locale.ROOT);
                    String mediaUrl = String.valueOf(media.getOrDefault("media_url", "")).trim();
                    if (!StringUtils.hasText(mediaUrl)) continue;

                    if ("IMAGE".equals(mediaType)) {
                        appendImageUrl(imageUrlMap, mediaUrl);
                        continue;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", media.get("id"));
                    item.put("spu_id", media.get("spu_id"));
                    item.put("media_type", mediaType);
                    item.put("media_url", mediaUrl);
                    item.put("sort_no", media.get("sort_no"));
                    mediaList.add(item);
                }
                if (!mediaList.isEmpty()) {
                    mapped.put("media_list", mediaList);
                }
            }
        }

        List<String> imageList = new ArrayList<>(imageUrlMap.values());
        mapped.put("image_urls", imageList);
        if (!imageList.isEmpty()) {
            mapped.put("main_image_url", imageList.get(0));
        }
        return mapped;
    }

    private void appendImageUrl(Map<String, String> container, Object rawValue) {
        if (container == null || rawValue == null) return;
        String text = String.valueOf(rawValue).trim();
        if (!StringUtils.hasText(text)) return;
        String key = normalizeImageComparableKey(text);
        String value = normalizeImageOutputUrl(text);
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) return;
        container.putIfAbsent(key, value);
    }

    private void appendImageUrlsFromRaw(Object rawValue, Map<String, String> container) {
        if (container == null || rawValue == null) return;
        if (rawValue instanceof Collection<?>) {
            for (Object item : (Collection<?>) rawValue) {
                appendImageUrl(container, item);
            }
            return;
        }
        if (rawValue.getClass().isArray()) {
            int length = Array.getLength(rawValue);
            for (int i = 0; i < length; i++) {
                appendImageUrl(container, Array.get(rawValue, i));
            }
            return;
        }
        String text = String.valueOf(rawValue).trim();
        if (!StringUtils.hasText(text)) return;
        if (text.startsWith("[") && text.endsWith("]")) {
            String body = text.substring(1, text.length() - 1);
            if (StringUtils.hasText(body)) {
                String[] parts = body.split(",");
                for (String part : parts) {
                    String value = part == null ? "" : part.trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    appendImageUrl(container, value);
                }
                return;
            }
        }
        appendImageUrl(container, text);
    }

    private String normalizeImageComparableKey(String rawUrl) {
        String text = String.valueOf(rawUrl == null ? "" : rawUrl).trim();
        if (!StringUtils.hasText(text)) return "";
        java.util.regex.Matcher matcher = ADMIN_FILE_CONTENT_URL_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        if (text.startsWith("/api/v1/admin/files/")) {
            int end = text.length();
            int q = text.indexOf('?');
            int h = text.indexOf('#');
            if (q >= 0) end = Math.min(end, q);
            if (h >= 0) end = Math.min(end, h);
            return text.substring(0, end);
        }
        return text;
    }

    private String normalizeImageOutputUrl(String rawUrl) {
        String text = String.valueOf(rawUrl == null ? "" : rawUrl).trim();
        if (!StringUtils.hasText(text)) return "";
        String key = normalizeImageComparableKey(text);
        if (StringUtils.hasText(key) && key.startsWith("/api/v1/admin/files/")) {
            return key;
        }
        return text;
    }

    private Long requireUserId(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return userId;
    }

    private void ensureUserActive(Long userId) {
        if (userId == null || userId <= 0) return;
        Map<String, Object> user = store.getUser(userId);
        String status = user == null ? "ACTIVE" : String.valueOf(user.getOrDefault("user_status_code", "ACTIVE"));
        if ("FROZEN".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(FORBIDDEN, "账号已冻结，请联系管理员");
        }
    }

    private Long resolveUserIdOrDefault(HttpServletRequest request) {
        return requireUserId(request);
    }

    private Long resolveUserIdIfPresent(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) return null;
        return resolveUserIdFromToken(token);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return "";
        }
        return auth.substring("Bearer ".length()).trim();
    }

    private Long resolveUserIdFromToken(String token) {
        if (!StringUtils.hasText(token)) return null;
        if (jwtTokenService != null) {
            Long userId = jwtTokenService.parseAppAccessUserId(token);
            if (userId != null) return userId;
        }
        if (allowLegacyToken) {
            return store.getUserIdByToken(token);
        }
        return null;
    }

    private boolean matchesProductCategory(Map<String, Object> product, Long categoryId) {
        if (product == null || categoryId == null) return false;
        Long primary = InMemoryData.toLong(product.getOrDefault("category_id", 0));
        if (Objects.equals(primary, categoryId)) return true;
        Object raw = product.get("category_ids");
        if (raw instanceof Collection<?>) {
            for (Object item : (Collection<?>) raw) {
                if (Objects.equals(InMemoryData.toLong(item), categoryId)) return true;
            }
        } else if (raw instanceof String) {
            String[] split = String.valueOf(raw).split("[,，\\s]+");
            for (String part : split) {
                if (!StringUtils.hasText(part)) continue;
                try {
                    if (Objects.equals(Long.parseLong(part.trim()), categoryId)) return true;
                } catch (Exception ignore) {
                    // ignore invalid token
                }
            }
        }
        return false;
    }

    private List<Map<String, Object>> resolveHomeRecommendProducts() {
        List<Map<String, Object>> allProducts = store.listProducts().stream()
                .filter(item -> "ON_SHELF".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ON_SHELF"))))
                .collect(Collectors.toList());
        if (allProducts.isEmpty()) return new ArrayList<>();

        final int homeLimit = 14;
        if (adminBusinessController == null) {
            return new ArrayList<>();
        }

        Set<Long> homeSlotIds = adminBusinessController.snapshotRecommendSlots().stream()
                .filter(slot -> "ENABLED".equalsIgnoreCase(String.valueOf(slot.getOrDefault("status_code", "ENABLED"))))
                .filter(slot -> {
                    String code = String.valueOf(slot.getOrDefault("slot_code", "")).trim().toUpperCase(Locale.ROOT);
                    return "HOME_RECOMMEND".equals(code);
                })
                .map(slot -> InMemoryData.toLong(slot.getOrDefault("id", 0)))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (homeSlotIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Map<String, Object>> productById = allProducts.stream()
                .collect(Collectors.toMap(
                        item -> InMemoryData.toLong(item.getOrDefault("id", 0)),
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<Long> orderedSpuIds = adminBusinessController.snapshotRecommendItems().stream()
                .filter(item -> homeSlotIds.contains(InMemoryData.toLong(item.getOrDefault("slot_id", 0))))
                .filter(item -> "ENABLED".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ENABLED"))))
                .sorted((a, b) -> Integer.compare(
                        InMemoryData.toInt(b.getOrDefault("sort_no", 0)),
                        InMemoryData.toInt(a.getOrDefault("sort_no", 0))
                ))
                .map(item -> InMemoryData.toLong(item.getOrDefault("spu_id", 0)))
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long spuId : orderedSpuIds) {
            Map<String, Object> target = productById.get(spuId);
            if (target == null) continue;
            result.add(target);
            if (result.size() >= homeLimit) break;
        }
        return result;
    }

    private Map<String, Object> buildLoginData(Long userId, boolean needBindPhone, boolean officialEnabled) {
        String accessToken = buildAccessToken(userId);
        String refreshToken = buildRefreshToken(userId);
        Map<String, Object> user = store.getUser(userId);
        String phoneMasked = user == null ? "" : String.valueOf(user.getOrDefault("phone_masked", ""));

        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("access_token", accessToken);
        data.put("refresh_token", refreshToken);
        data.put("phone_masked", phoneMasked);
        data.put("is_new_user", needBindPhone);
        data.put("need_bind_phone", needBindPhone);
        data.put("official_enabled", officialEnabled);
        return data;
    }

    private String buildAccessToken(Long userId) {
        if (jwtTokenService != null) {
            String token = jwtTokenService.issueAppAccessToken(userId);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return allowLegacyToken ? store.issueToken(userId) : "";
    }

    private String buildRefreshToken(Long userId) {
        if (jwtTokenService != null) {
            String token = jwtTokenService.issueAppRefreshToken(userId);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return allowLegacyToken ? store.issueRefreshToken(userId) : "";
    }

    private <T> T cacheOrLoad(String key,
                              com.fasterxml.jackson.core.type.TypeReference<T> typeReference,
                              Duration ttl,
                              java.util.function.Supplier<T> loader) {
        if (redisCacheService == null) return loader.get();
        return redisCacheService.getOrLoad(key, ttl, typeReference, loader);
    }

    private void syncAdminSnapshotIfPossible() {
        if (adminBusinessController == null) return;
        try {
            adminBusinessController.syncAndPersistFromMiniProgram();
        } catch (Exception ignore) {
            // 不影响主流程
        }
    }

    private void normalizeNickNameAfterPhoneBind(Map<String, Object> user) {
        if (user == null) return;
        String currentNick = String.valueOf(user.getOrDefault("nick_name", "")).trim();
        if (!isGenericWechatNick(currentNick)) return;
        String fallback = buildPhoneTailNick(user);
        if (StringUtils.hasText(fallback)) {
            user.put("nick_name", fallback);
        }
    }

    private String normalizeNickNameInput(String nickName, Map<String, Object> user) {
        String normalized = String.valueOf(nickName == null ? "" : nickName).trim();
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (isGenericWechatNick(normalized)) {
            String fallback = buildPhoneTailNick(user);
            if (StringUtils.hasText(fallback)) {
                return fallback;
            }
        }
        return normalized;
    }

    private boolean isGenericWechatNick(String nickName) {
        if (!StringUtils.hasText(nickName)) return false;
        return WECHAT_DEFAULT_NICK.matcher(nickName.trim()).matches();
    }

    private String buildPhoneTailNick(Map<String, Object> user) {
        if (user == null) return "";
        String phone = String.valueOf(user.getOrDefault("phone", ""));
        if (!StringUtils.hasText(phone)) {
            phone = String.valueOf(user.getOrDefault("phone_masked", ""));
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return "用户" + digits.substring(digits.length() - 4);
        }
        Long userId = InMemoryData.toLong(user.get("id"));
        return userId == null ? "用户" : "用户" + userId;
    }
}
