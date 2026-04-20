package com.integral.mall.api.controller;

import com.integral.mall.api.common.ApiResponse;
import com.integral.mall.api.service.JwtTokenService;
import com.integral.mall.api.store.InMemoryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private final Map<String, Long> adminAccessTokenMap = new ConcurrentHashMap<>();
    private final Map<String, Long> adminRefreshTokenMap = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> operationLogs = new ArrayList<>();
    private final List<Map<String, Object>> outboxEvents = new ArrayList<>();

    @Value("${mall.bootstrap-mock-data:false}")
    private boolean bootstrapMockData;
    @Value("${mall.security.allow-legacy-token:false}")
    private boolean allowLegacyToken;

    @Autowired
    private AdminBusinessController adminBusinessController;
    @Autowired(required = false)
    private JwtTokenService jwtTokenService;

    @PostConstruct
    public void initMockMonitorData() {
        if (!bootstrapMockData) return;
        operationLogs.add(log(10001L, "2026-03-17 10:03:12", "登录鉴权", "管理员登录", "admin", "SUCCESS", "POST", "/api/v1/admin/auth/login",
                "10.0.0.23", "req_20260317_001", "管理员登录成功"));
        operationLogs.add(log(10002L, "2026-03-17 10:15:20", "订单中心", "审核通过", "admin", "SUCCESS", "POST", "/api/v1/admin/orders/30001/approve",
                "10.0.0.23", "req_20260317_002", "订单 30001 审核通过"));
        operationLogs.add(log(10003L, "2026-03-17 10:18:48", "商品中心", "更新库存", "admin", "SUCCESS", "PUT", "/api/v1/admin/products/skus/5001/stock",
                "10.0.0.17", "req_20260317_003", "SKU 5001 库存 +100"));
        operationLogs.add(log(10004L, "2026-03-17 10:20:53", "用户管理", "冻结用户", "admin", "FAILED", "PUT", "/api/v1/admin/users/1003/status",
                "10.0.0.31", "req_20260317_004", "用户状态更新失败"));
        operationLogs.add(log(10005L, "2026-03-17 10:30:31", "分类管理", "新增分类", "admin", "SUCCESS", "POST", "/api/v1/admin/categories",
                "10.0.0.23", "req_20260317_005", "新增分类：潮流周边"));
        operationLogs.add(log(10006L, "2026-03-17 10:45:02", "推荐位", "推荐排序", "admin", "SUCCESS", "PUT", "/api/v1/admin/recommend-slots/201/items/sort",
                "10.0.0.23", "req_20260317_006", "首页推荐排序已更新"));

        outboxEvents.add(outbox(81001L, "ORDER_STATUS_CHANGED", "ORDER", "30001", "SUCCESS", 0, "", "", "2026-03-17 10:16:00", "2026-03-17 10:16:00"));
        outboxEvents.add(outbox(81002L, "POINT_LEDGER_CHANGED", "POINT", "50001", "PENDING", 1, "2026-03-17 10:40:00", "", "2026-03-17 10:17:30", "2026-03-17 10:17:30"));
        outboxEvents.add(outbox(81003L, "ASSET_GRANTED", "ASSET", "70001", "FAILED", 2, "2026-03-17 10:35:00", "Kafka timeout", "2026-03-17 10:19:11", "2026-03-17 10:19:11"));
        outboxEvents.add(outbox(81004L, "GROUP_RESOURCE_EXCHANGED", "GROUP_RESOURCE", "9001", "SUCCESS", 0, "", "", "2026-03-17 10:21:44", "2026-03-17 10:21:44"));
        outboxEvents.add(outbox(81005L, "ORDER_REJECTED", "ORDER", "30002", "PENDING", 0, "2026-03-17 10:45:00", "", "2026-03-17 10:24:28", "2026-03-17 10:24:28"));
    }

    // =========================
    // auth
    // =========================
    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> payload) {
        String username = payload == null ? "" : String.valueOf(payload.getOrDefault("username", "")).trim();
        String password = payload == null ? "" : String.valueOf(payload.getOrDefault("password", ""));

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return ApiResponse.fail(4001, "账号或密码不能为空");
        }

        Map<String, Object> adminUser = adminBusinessController.authenticateAdmin(username, password);
        if (adminUser == null) {
            return ApiResponse.fail(4010, "账号或密码错误");
        }

        Long adminId = InMemoryData.toLong(adminUser.get("id"));

        String accessToken = issueAccessToken(adminId);
        String refreshToken = issueRefreshToken(adminId);
        if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(refreshToken)) {
            return ApiResponse.fail(5002, "JWT 服务不可用，请检查 mall.jwt.secret 配置");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("refresh_token", refreshToken);
        data.put("admin_user", adminUser);
        return ApiResponse.ok(data);
    }

    @PostMapping("/auth/refresh-token")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody(required = false) Map<String, Object> payload) {
        String refreshToken = payload == null ? "" : String.valueOf(payload.getOrDefault("refresh_token", ""));
        Long adminId = jwtTokenService == null ? null : jwtTokenService.parseAdminRefreshId(refreshToken);
        if (adminId == null && allowLegacyToken) {
            adminId = adminRefreshTokenMap.get(refreshToken);
        }
        if (adminId == null) {
            return ApiResponse.fail(4011, "refresh_token 无效");
        }
        Map<String, Object> adminUser = adminBusinessController.getAdminUserSafeById(adminId);
        if (adminUser == null) {
            return ApiResponse.fail(4012, "管理员不存在");
        }
        Map<String, Object> data = new HashMap<>();
        String accessToken = issueAccessToken(adminId);
        String nextRefreshToken = issueRefreshToken(adminId);
        if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(nextRefreshToken)) {
            return ApiResponse.fail(5002, "JWT 服务不可用，请检查 mall.jwt.secret 配置");
        }
        data.put("access_token", accessToken);
        data.put("refresh_token", nextRefreshToken);
        data.put("admin_user", adminUser);
        return ApiResponse.ok(data);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (StringUtils.hasText(token)) {
            adminAccessTokenMap.remove(token);
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/auth/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        Long adminId = requireAdminId(request);
        Map<String, Object> adminUser = adminBusinessController.getAdminUserSafeById(adminId);
        if (adminUser == null) {
            return ApiResponse.fail(4041, "管理员不存在");
        }
        return ApiResponse.ok(adminUser);
    }

    @PutMapping("/auth/password")
    public ApiResponse<Map<String, Object>> updatePassword(HttpServletRequest request,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        Long adminId = requireAdminId(request);
        String oldPassword = payload == null ? "" : String.valueOf(payload.getOrDefault("old_password", ""));
        String newPassword = payload == null ? "" : String.valueOf(payload.getOrDefault("new_password", ""));

        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            return ApiResponse.fail(4003, "新密码长度不能少于 6 位");
        }
        if (!adminBusinessController.updateAdminPassword(adminId, oldPassword, newPassword)) {
            return ApiResponse.fail(4002, "旧密码不正确");
        }
        adminBusinessController.persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/admin-users/{adminUserId}/password")
    public ApiResponse<Map<String, Object>> viewAdminPassword(HttpServletRequest request, @PathVariable Long adminUserId) {
        Long adminId = requireAdminId(request);
        if (!adminBusinessController.isSuperAdmin(adminId)) {
            return ApiResponse.fail(4031, "仅超级管理员可查看密码");
        }
        String password = adminBusinessController.getAdminPassword(adminUserId);
        if (!StringUtils.hasText(password)) {
            return ApiResponse.fail(4041, "管理员不存在");
        }
        return ApiResponse.ok(Map.of("password", password));
    }

    // =========================
    // dashboard & monitor
    // =========================
    @GetMapping("/dashboard/overview")
    public ApiResponse<Map<String, Object>> dashboardOverview(HttpServletRequest request) {
        requireAdminId(request);
        List<Map<String, Object>> users = adminBusinessController.snapshotUsers();
        List<Map<String, Object>> orders = adminBusinessController.snapshotOrders();
        List<Map<String, Object>> products = adminBusinessController.snapshotSpus();

        LocalDate today = LocalDate.now(BEIJING_ZONE);
        long todayExchangePoint = orders.stream()
                .filter(item -> isSameDate(String.valueOf(item.get("submit_at")), today))
                .mapToLong(item -> InMemoryData.toLong(item.get("total_point_amount")))
                .sum();
        long pendingWriteoffPoint = users.stream()
                .filter(item -> !"FROZEN".equalsIgnoreCase(String.valueOf(item.getOrDefault("user_status_code", "ACTIVE"))))
                .mapToLong(item -> Math.max(0L, InMemoryData.toLong(item.getOrDefault("point_balance", 0))))
                .sum();

        Map<String, Object> data = new HashMap<>();
        data.put("user_total", users.size());
        data.put("order_total", orders.size());
        data.put("pending_audit_count", orders.stream()
                .filter(item -> "PENDING_AUDIT".equals(String.valueOf(item.get("order_status_code"))))
                .count());
        data.put("today_exchange_point", todayExchangePoint);
        data.put("pending_writeoff_point", pendingWriteoffPoint);
        data.put("product_total", products.size());
        return ApiResponse.ok(data);
    }

    @GetMapping("/stats/users")
    public ApiResponse<Map<String, Object>> userStats(HttpServletRequest request) {
        requireAdminId(request);
        List<Map<String, Object>> users = adminBusinessController.snapshotUsers();
        List<Map<String, Object>> orders = adminBusinessController.snapshotOrders();

        LocalDate today = LocalDate.now(BEIJING_ZONE);
        LocalDate sevenDaysAgo = today.minusDays(6);

        long newUserToday = users.stream()
                .filter(item -> isSameDate(String.valueOf(item.get("created_at")), today))
                .count();

        Set<Long> activeUsers7d = orders.stream()
                .filter(item -> inDateRange(String.valueOf(item.get("submit_at")), sevenDaysAgo, today))
                .map(item -> InMemoryData.toLong(item.get("user_id")))
                .filter(id -> id > 0)
                .collect(Collectors.toSet());

        long frozenUserCount = users.stream()
                .filter(item -> "FROZEN".equals(String.valueOf(item.get("user_status_code"))))
                .count();

        double conversionRate = users.isEmpty() ? 0D : activeUsers7d.size() / (double) users.size();

        Map<String, Object> data = new HashMap<>();
        data.put("new_user_today", newUserToday);
        data.put("active_user_7d", activeUsers7d.size());
        data.put("frozen_user_count", frozenUserCount);
        data.put("conversion_rate", conversionRate);
        return ApiResponse.ok(data);
    }

    @GetMapping("/stats/orders")
    public ApiResponse<Map<String, Object>> orderStats(HttpServletRequest request) {
        requireAdminId(request);
        List<Map<String, Object>> orders = adminBusinessController.snapshotOrders();
        Map<String, Object> data = new HashMap<>();
        data.put("pending_audit_count", countOrderByStatus(orders, "PENDING_AUDIT"));
        data.put("pending_ship_count", countOrderByStatus(orders, "PENDING_SHIP"));
        data.put("shipped_count", countOrderByStatus(orders, "SHIPPED"));
        data.put("finished_count", countOrderByStatus(orders, "FINISHED"));
        data.put("close_count", countOrderByStatus(orders, "CLOSED"));
        return ApiResponse.ok(data);
    }

    @GetMapping("/stats/products")
    public ApiResponse<Map<String, Object>> productStats(HttpServletRequest request) {
        requireAdminId(request);
        List<Map<String, Object>> products = adminBusinessController.snapshotSpus();
        long onShelfCount = products.stream()
                .filter(item -> "ON_SHELF".equals(String.valueOf(item.get("status_code"))))
                .count();
        long offShelfCount = products.size() - onShelfCount;
        long recommendCount = products.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("recommend_flag")))
                .count();
        long lowStockCount = products.stream()
                .filter(item -> InMemoryData.toLong(item.get("total_stock")) <= 10)
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("on_shelf_count", onShelfCount);
        data.put("off_shelf_count", offShelfCount);
        data.put("recommend_count", recommendCount);
        data.put("low_stock_count", lowStockCount);
        return ApiResponse.ok(data);
    }

    @GetMapping("/stats/conversions")
    public ApiResponse<Map<String, Object>> conversionStats(HttpServletRequest request) {
        requireAdminId(request);
        List<Map<String, Object>> users = adminBusinessController.snapshotUsers();
        List<Map<String, Object>> orders = adminBusinessController.snapshotOrders();
        LocalDate today = LocalDate.now(BEIJING_ZONE);

        Set<Long> orderUsersToday = orders.stream()
                .filter(item -> isSameDate(String.valueOf(item.get("submit_at")), today))
                .map(item -> InMemoryData.toLong(item.get("user_id")))
                .filter(id -> id > 0)
                .collect(Collectors.toSet());

        Set<Long> newUsersToday = users.stream()
                .filter(item -> isSameDate(String.valueOf(item.get("created_at")), today))
                .map(item -> InMemoryData.toLong(item.get("id")))
                .filter(id -> id > 0)
                .collect(Collectors.toSet());

        Set<Long> visitUsersToday = new LinkedHashSet<>(orderUsersToday);
        visitUsersToday.addAll(newUsersToday);

        long todayOrderCount = orders.stream()
                .filter(item -> isSameDate(String.valueOf(item.get("submit_at")), today))
                .count();
        long todayPointSum = orders.stream()
                .filter(item -> isSameDate(String.valueOf(item.get("submit_at")), today))
                .mapToLong(item -> InMemoryData.toLong(item.get("total_point_amount")))
                .sum();

        long visitUvToday = visitUsersToday.size();
        long exchangeUvToday = orderUsersToday.size();
        double conversionRate = visitUvToday == 0 ? 0D : exchangeUvToday / (double) visitUvToday;
        long avgExchangePoint = todayOrderCount == 0 ? 0L : Math.round(todayPointSum / (double) todayOrderCount);

        Map<String, Object> data = new HashMap<>();
        data.put("visit_uv_today", visitUvToday);
        data.put("exchange_uv_today", exchangeUvToday);
        data.put("conversion_rate", conversionRate);
        data.put("avg_exchange_point", avgExchangePoint);
        return ApiResponse.ok(data);
    }

    @GetMapping("/operation-logs")
    public ApiResponse<Map<String, Object>> operationLogs(HttpServletRequest request,
                                                          @RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "20") int pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(name = "status_code", required = false) String statusCode) {
        requireAdminId(request);
        List<Map<String, Object>> source = buildRealtimeOperationLogs();
        List<Map<String, Object>> filtered = source.stream()
                .filter(item -> !StringUtils.hasText(statusCode) || statusCode.equals(item.get("status_code")))
                .filter(item -> !StringUtils.hasText(keyword) || containsAny(item, keyword,
                        "module_name", "action_name", "operator_name", "request_path", "request_id", "detail_text"))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(filtered, pageNo, pageSize));
    }

    @GetMapping("/operation-logs/{logId}")
    public ApiResponse<Map<String, Object>> operationLogDetail(HttpServletRequest request, @PathVariable Long logId) {
        requireAdminId(request);
        return buildRealtimeOperationLogs().stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("id")), logId))
                .findFirst()
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail(4041, "日志不存在"));
    }

    @GetMapping("/outbox-events")
    public ApiResponse<Map<String, Object>> outboxEvents(HttpServletRequest request,
                                                         @RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(name = "status_code", required = false) String statusCode) {
        requireAdminId(request);
        List<Map<String, Object>> source = buildRealtimeOutboxEvents();
        List<Map<String, Object>> filtered = source.stream()
                .filter(item -> !StringUtils.hasText(statusCode) || statusCode.equals(item.get("status_code")))
                .filter(item -> !StringUtils.hasText(keyword) || containsAny(item, keyword,
                        "event_type_code", "aggregate_type_code", "aggregate_id", "last_error_msg"))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(filtered, pageNo, pageSize));
    }

    @PostMapping("/outbox-events/{eventId}/retry")
    public ApiResponse<Map<String, Object>> retryOutboxEvent(HttpServletRequest request, @PathVariable Long eventId) {
        requireAdminId(request);
        Map<String, Object> target = buildRealtimeOutboxEvents().stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("id")), eventId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return ApiResponse.fail(4042, "事件不存在");
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // helper
    // =========================
    private String issueAccessToken(Long adminId) {
        if (jwtTokenService != null) {
            String jwt = jwtTokenService.issueAdminAccessToken(adminId);
            if (StringUtils.hasText(jwt)) {
                return jwt;
            }
        }
        if (!allowLegacyToken) {
            return "";
        }
        String token = "admin-token-" + UUID.randomUUID().toString().replace("-", "");
        adminAccessTokenMap.put(token, adminId);
        return token;
    }

    private String issueRefreshToken(Long adminId) {
        if (jwtTokenService != null) {
            String jwt = jwtTokenService.issueAdminRefreshToken(adminId);
            if (StringUtils.hasText(jwt)) {
                return jwt;
            }
        }
        if (!allowLegacyToken) {
            return "";
        }
        String token = "admin-refresh-" + UUID.randomUUID().toString().replace("-", "");
        adminRefreshTokenMap.put(token, adminId);
        return token;
    }

    public Long resolveAdminId(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if (jwtTokenService != null) {
            Long adminId = jwtTokenService.parseAdminAccessId(token);
            if (adminId != null) {
                return adminId;
            }
        }
        return allowLegacyToken ? adminAccessTokenMap.get(token) : null;
    }

    public boolean isSuperAdmin(HttpServletRequest request) {
        Long adminId = resolveAdminId(request);
        if (adminId == null) return false;
        return adminBusinessController.isSuperAdmin(adminId);
    }

    private Long requireAdminId(HttpServletRequest request) {
        Long adminId = resolveAdminId(request);
        if (adminId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return adminId;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return "";
        }
        return auth.substring("Bearer ".length()).trim();
    }

    private boolean containsAny(Map<String, Object> item, String keyword, String... fields) {
        for (String field : fields) {
            Object value = item.get(field);
            if (value != null && String.valueOf(value).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> source, int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int from = Math.max((safePageNo - 1) * safePageSize, 0);
        int to = Math.min(from + safePageSize, source.size());
        List<Map<String, Object>> list = from >= source.size() ? new ArrayList<>() : source.subList(from, to);

        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", safePageNo);
        data.put("pageSize", safePageSize);
        data.put("total", source.size());
        data.put("list", list);
        return data;
    }

    private long countOrderByStatus(List<Map<String, Object>> orders, String status) {
        return orders.stream()
                .filter(item -> status.equals(String.valueOf(item.get("order_status_code"))))
                .count();
    }

    private List<Map<String, Object>> buildRealtimeOperationLogs() {
        List<Map<String, Object>> logs = new ArrayList<>();

        List<Map<String, Object>> orderFlows = adminBusinessController.snapshotOrderFlows();
        for (Map<String, Object> flow : orderFlows) {
            Long id = InMemoryData.toLong(flow.get("id"));
            Long orderId = InMemoryData.toLong(flow.get("order_id"));
            String action = String.valueOf(flow.getOrDefault("action_text", "状态流转"));
            String operator = String.valueOf(flow.getOrDefault("operator_name", "系统"));
            String occurredAt = String.valueOf(flow.getOrDefault("occurred_at", now()));
            String note = String.valueOf(flow.getOrDefault("note", ""));
            String fromStatus = String.valueOf(flow.getOrDefault("from_status", ""));
            String toStatus = String.valueOf(flow.getOrDefault("to_status", ""));

            logs.add(log(
                    id > 0 ? id : Math.abs(UUID.randomUUID().getMostSignificantBits()),
                    occurredAt,
                    "订单中心",
                    action,
                    operator,
                    "SUCCESS",
                    "POST",
                    "/api/v1/admin/orders/" + orderId,
                    "127.0.0.1",
                    "req_order_" + orderId + "_" + id,
                    StringUtils.hasText(note) ? note : (fromStatus + " -> " + toStatus)
            ));
        }

        List<Map<String, Object>> ledgers = adminBusinessController.snapshotPointLedger();
        for (Map<String, Object> ledger : ledgers) {
            long id = 900000L + InMemoryData.toLong(ledger.get("id"));
            long userId = InMemoryData.toLong(ledger.get("user_id"));
            String occurredAt = String.valueOf(ledger.getOrDefault("occurred_at", now()));
            long delta = InMemoryData.toLong(ledger.get("change_amount"));
            String action = delta >= 0 ? "积分增加" : "积分扣减";
            logs.add(log(
                    id,
                    occurredAt,
                    "用户管理",
                    action,
                    "系统",
                    "SUCCESS",
                    "POST",
                    "/api/v1/admin/users/" + userId + "/points/adjust",
                    "127.0.0.1",
                    "req_point_" + id,
                    "积分变动：" + delta
            ));
        }

        logs.sort((a, b) -> String.valueOf(b.get("occurred_at")).compareTo(String.valueOf(a.get("occurred_at"))));
        return logs;
    }

    private List<Map<String, Object>> buildRealtimeOutboxEvents() {
        List<Map<String, Object>> events = new ArrayList<>();

        List<Map<String, Object>> orders = adminBusinessController.snapshotOrders();
        for (Map<String, Object> order : orders) {
            long orderId = InMemoryData.toLong(order.get("id"));
            String status = String.valueOf(order.getOrDefault("order_status_code", "PENDING_AUDIT"));
            String submitAt = String.valueOf(order.getOrDefault("submit_at", now()));
            String eventStatus = "SUCCESS";
            if ("PENDING_AUDIT".equals(status) || "PENDING_SHIP".equals(status)) {
                eventStatus = "PENDING";
            }
            events.add(outbox(
                    orderId,
                    "ORDER_STATUS_CHANGED",
                    "ORDER",
                    String.valueOf(orderId),
                    eventStatus,
                    0,
                    "",
                    "",
                    submitAt,
                    submitAt
            ));
        }

        List<Map<String, Object>> ledgers = adminBusinessController.snapshotPointLedger();
        for (Map<String, Object> ledger : ledgers) {
            long ledgerId = InMemoryData.toLong(ledger.get("id"));
            String occurredAt = String.valueOf(ledger.getOrDefault("occurred_at", now()));
            events.add(outbox(
                    1000000L + ledgerId,
                    "POINT_LEDGER_CHANGED",
                    "POINT",
                    String.valueOf(ledgerId),
                    "SUCCESS",
                    0,
                    "",
                    "",
                    occurredAt,
                    occurredAt
            ));
        }

        events.sort((a, b) -> String.valueOf(b.get("updated_at")).compareTo(String.valueOf(a.get("updated_at"))));
        return events;
    }

    private boolean isSameDate(String dateTimeText, LocalDate targetDate) {
        LocalDate date = parseDate(dateTimeText);
        return date != null && Objects.equals(date, targetDate);
    }

    private boolean inDateRange(String dateTimeText, LocalDate startDate, LocalDate endDate) {
        LocalDate date = parseDate(dateTimeText);
        if (date == null) return false;
        return (!date.isBefore(startDate)) && (!date.isAfter(endDate));
    }

    private LocalDate parseDate(String dateTimeText) {
        if (!StringUtils.hasText(dateTimeText)) return null;
        try {
            return LocalDateTime.parse(dateTimeText, DT).toLocalDate();
        } catch (Exception ignore) {
            if (dateTimeText.length() >= 10) {
                try {
                    return LocalDate.parse(dateTimeText.substring(0, 10));
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private Map<String, Object> log(Long id, String occurredAt, String moduleName, String actionName, String operatorName,
                                    String statusCode, String requestMethod, String requestPath,
                                    String clientIp, String requestId, String detailText) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("occurred_at", occurredAt);
        item.put("module_name", moduleName);
        item.put("action_name", actionName);
        item.put("operator_name", operatorName);
        item.put("status_code", statusCode);
        item.put("request_method", requestMethod);
        item.put("request_path", requestPath);
        item.put("client_ip", clientIp);
        item.put("request_id", requestId);
        item.put("detail_text", detailText);
        return item;
    }

    private Map<String, Object> outbox(Long id, String eventTypeCode, String aggregateTypeCode, String aggregateId,
                                       String statusCode, int retryCount, String nextRetryAt,
                                       String lastErrorMsg, String createdAt, String updatedAt) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("event_type_code", eventTypeCode);
        item.put("aggregate_type_code", aggregateTypeCode);
        item.put("aggregate_id", aggregateId);
        item.put("status_code", statusCode);
        item.put("retry_count", retryCount);
        item.put("next_retry_at", nextRetryAt);
        item.put("last_error_msg", lastErrorMsg);
        item.put("created_at", createdAt);
        item.put("updated_at", updatedAt);
        return item;
    }

    private String now() {
        return LocalDateTime.now(BEIJING_ZONE).format(DT);
    }
}
