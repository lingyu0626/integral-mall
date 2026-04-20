package com.integral.mall.api.store;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class InMemoryData {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private final AtomicLong addressIdSeq = new AtomicLong(20000);
    private final AtomicLong orderIdSeq = new AtomicLong(30000);
    private final AtomicLong orderNoSeq = new AtomicLong(100000);
    private final AtomicLong ledgerIdSeq = new AtomicLong(50000);
    private final AtomicLong assetFlowIdSeq = new AtomicLong(70000);
    private final AtomicLong userIdSeq = new AtomicLong(1000);

    private final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final Map<String, Long> wxOpenIdToUserId = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshToUserId = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> pointAccounts = new ConcurrentHashMap<>();

    private final Map<Long, LinkedHashMap<Long, Map<String, Object>>> addresses = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> categories = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> products = new LinkedHashMap<>();
    private final List<String> homeBanners = new ArrayList<>();

    private final Map<Long, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> orderItems = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> orderFlows = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> orderDeliveries = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> orderAddressSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Long> idempotentOrderMap = new ConcurrentHashMap<>();

    private final Map<Long, List<Map<String, Object>>> pointLedgers = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> assets = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> assetFlows = new ConcurrentHashMap<>();

    private final Map<Long, Map<String, Object>> groupResources = new LinkedHashMap<>();
    private final Map<String, List<Map<String, Object>>> dictItems = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> publicConfigs = new ArrayList<>();
    private final Map<String, Object> customerServiceContact = new ConcurrentHashMap<>();

    @Value("${mall.bootstrap-mock-data:false}")
    private boolean bootstrapMockData;
    @Value("${mall.require-mysql:false}")
    private boolean requireMysql;

    public InMemoryData() {}

    @PostConstruct
    public void setup() {
        if (!requireMysql) {
            initCoreUserData();
        }
        if (bootstrapMockData) {
            initMockBusinessData();
        }
        syncUserIdSeq();
    }

    public static String now() {
        return LocalDateTime.now(BEIJING_ZONE).format(DT);
    }

    private void initCoreUserData() {
        Map<String, Object> user = new HashMap<>();
        user.put("id", 1001L);
        user.put("user_no", "U1001");
        user.put("nick_name", "碎片用户");
        user.put("avatar_url", "https://dummyimage.com/128x128/e8ebf1/7f8796&text=U");
        user.put("phone", "13812345678");
        user.put("phone_masked", maskPhone("13812345678"));
        user.put("user_status_code", "ACTIVE");
        user.put("created_at", now());
        users.put(1001L, user);

        Map<String, Object> pointAccount = new HashMap<>();
        pointAccount.put("id", 1L);
        pointAccount.put("user_id", 1001L);
        pointAccount.put("point_balance", 0L);
        pointAccount.put("point_total_income", 0L);
        pointAccount.put("point_total_expense", 0L);
        pointAccount.put("account_status_code", "ACTIVE");
        pointAccounts.put(1001L, pointAccount);
        pointLedgers.put(1001L, new ArrayList<>());
    }

    private void initMockBusinessData() {
        Map<String, Object> pointAccount = pointAccounts.get(1001L);
        if (pointAccount != null) {
            pointAccount.put("point_balance", 16888L);
            pointAccount.put("point_total_income", 20000L);
            pointAccount.put("point_total_expense", 3112L);
        }
        addPointLedger(1001L, "INIT", 16888L, "初始碎片");

        addCategory(1L, "白酒", 1);
        addCategory(2L, "数码", 2);
        addCategory(3L, "生活权益", 3);

        addProduct(101L, 1L, "飞天茅台53度 500ML×1", "PHYSICAL", 1888L, 35L,
                "https://dummyimage.com/640x420/f4f5f7/7a8291&text=Moutai", "经典酱香型白酒，限量兑换");
        addProduct(102L, 2L, "苹果 Watch Ultra3", "PHYSICAL", 5988L, 18L,
                "https://dummyimage.com/640x420/f2f5f8/7a8291&text=Watch", "旗舰智能手表，支持多场景运动");
        addProduct(103L, 3L, "至臻玩家微信群入群资格", "VIRTUAL", 8888L, 9999L,
                "https://dummyimage.com/640x420/f8f3f1/7a8291&text=Group+VIP", "兑换后可获取入群二维码");
        addProduct(104L, 3L, "每天发红包牛票", "VIRTUAL", 888L, 9999L,
                "https://dummyimage.com/640x420/f8f0f1/7a8291&text=Ticket", "活动类虚拟权益");

        homeBanners.add("https://dummyimage.com/1200x480/f4f6f9/657083&text=Banner+1");
        homeBanners.add("https://dummyimage.com/1200x480/f8f4f7/657083&text=Banner+2");
        homeBanners.add("https://dummyimage.com/1200x480/f1f8f2/657083&text=Banner+3");

        addGroupResource(9001L, "至尊玩家群", "ACTIVE", 8888L,
                "https://dummyimage.com/480x480/f2f4f7/5f6878&text=VIP+GROUP");
        addGroupResource(9002L, "每日红包群", "ACTIVE", 888L,
                "https://dummyimage.com/480x480/f4f7f3/5f6878&text=RED+PACKET");

        List<Map<String, Object>> serviceHours = new ArrayList<>();
        serviceHours.add(dictItem(1L, "周一至周五 09:00 - 18:00"));
        serviceHours.add(dictItem(2L, "周六 10:00 - 16:00"));
        dictItems.put("SERVICE_HOURS", serviceHours);

        Map<String, Object> configNotice = new HashMap<>();
        configNotice.put("id", 1L);
        configNotice.put("config_key", "APP_NOTICE");
        configNotice.put("config_name", "平台公告");
        configNotice.put("config_value", "积分兑换订单将在 1-3 个工作日内处理");
        publicConfigs.add(configNotice);

        customerServiceContact.put("service_phone", "400-800-1234");
        customerServiceContact.put("wechat", "mall-kefu-01");
        customerServiceContact.put("contact_desc", "服务时段内可优先响应");
    }

    private void addCategory(Long id, String name, int sort) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("category_name", name);
        item.put("sort_no", sort);
        item.put("status_code", "ACTIVE");
        categories.put(id, item);
    }

    private void addProduct(Long id, Long categoryId, String productName, String productTypeCode, Long pointPrice, Long stock,
                            String imageUrl, String detailHtml) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("category_id", categoryId);
        item.put("product_name", productName);
        item.put("product_type_code", productTypeCode);
        item.put("point_price", pointPrice);
        item.put("stock_available", stock);
        item.put("main_image_url", imageUrl);
        item.put("detail_html", detailHtml);
        item.put("limit_per_user", 5);
        item.put("status_code", "ON_SHELF");
        products.put(id, item);
    }

    private void addAsset(Long id, String name, String typeCode, String statusCode, int quantity, String expireAt) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("user_id", 1001L);
        item.put("asset_name", name);
        item.put("asset_type_code", typeCode);
        item.put("asset_status_code", statusCode);
        item.put("quantity", quantity);
        item.put("expire_at", expireAt);
        item.put("cover_url", "https://dummyimage.com/360x360/f2f4f7/6b7382&text=Asset");
        assets.put(id, item);
        assetFlows.put(id, new ArrayList<>());
    }

    private void addGroupResource(Long id, String name, String statusCode, Long pointPrice, String qrUrl) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("resource_name", name);
        item.put("status_code", statusCode);
        item.put("point_price", pointPrice);
        item.put("qr_code_url", qrUrl);
        groupResources.put(id, item);
    }

    private Map<String, Object> dictItem(Long id, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("item_name", value);
        map.put("item_value", value);
        return map;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public Map<String, Object> getUser(Long userId) {
        return users.get(userId);
    }

    public Collection<Map<String, Object>> listUsers() {
        return new ArrayList<>(users.values());
    }

    public Map<String, Object> getPointAccount(Long userId) {
        return pointAccounts.get(userId);
    }

    public synchronized String issueToken(Long userId) {
        String token = "mock-token-" + UUID.randomUUID().toString().replace("-", "");
        tokenToUserId.put(token, userId);
        return token;
    }

    public synchronized String issueRefreshToken(Long userId) {
        String refresh = "mock-refresh-" + UUID.randomUUID().toString().replace("-", "");
        refreshToUserId.put(refresh, userId);
        return refresh;
    }

    public Long getUserIdByToken(String token) {
        return tokenToUserId.get(token);
    }

    public Long getUserIdByRefresh(String refreshToken) {
        return refreshToUserId.get(refreshToken);
    }

    public void removeToken(String token) {
        tokenToUserId.remove(token);
    }

    public Collection<Map<String, Object>> listCategories() {
        return categories.values();
    }

    public synchronized void upsertCategoryFromAdmin(Long id,
                                                     String categoryName,
                                                     Integer sortNo,
                                                     String adminStatus) {
        if (id == null) return;
        Map<String, Object> target = categories.computeIfAbsent(id, key -> new HashMap<>());
        target.put("id", id);
        if (StringUtils.hasText(categoryName)) {
            target.put("category_name", categoryName);
        }
        if (sortNo != null) {
            target.put("sort_no", sortNo);
        }
        if (StringUtils.hasText(adminStatus)) {
            target.put("status_code", mapCategoryStatusFromAdmin(adminStatus));
        }
        if (!target.containsKey("category_name")) {
            target.put("category_name", "未命名分类");
        }
        if (!target.containsKey("sort_no")) {
            target.put("sort_no", 100);
        }
        if (!target.containsKey("status_code")) {
            target.put("status_code", "ACTIVE");
        }
    }

    public synchronized void removeCategoryById(Long id) {
        if (id == null) return;
        categories.remove(id);
    }

    public List<Map<String, Object>> listProducts() {
        return new ArrayList<>(products.values());
    }

    public Map<String, Object> getProduct(Long productId) {
        return products.get(productId);
    }

    public synchronized void upsertProductFromAdmin(Long productId,
                                                    Long categoryId,
                                                    String productName,
                                                    String productTypeCode,
                                                    Long pointPrice,
                                                    Long stockAvailable,
                                                    String saleStatusCode,
                                                    String mainImageUrl,
                                                    String detailHtml,
                                                    Integer limitPerUser) {
        if (productId == null || productId <= 0) return;
        Map<String, Object> target = products.computeIfAbsent(productId, key -> new HashMap<>());
        target.put("id", productId);
        target.put("category_id", categoryId == null ? 0L : categoryId);
        if (categoryId != null && categoryId > 0) {
            target.put("category_ids", new ArrayList<>(List.of(categoryId)));
        }
        target.put("product_name", StringUtils.hasText(productName) ? productName : String.valueOf(target.getOrDefault("product_name", "未命名商品")));
        target.put("product_type_code", StringUtils.hasText(productTypeCode) ? productTypeCode : String.valueOf(target.getOrDefault("product_type_code", "PHYSICAL")));
        target.put("point_price", Math.max(0L, pointPrice == null ? 0L : pointPrice));
        target.put("stock_available", Math.max(0L, stockAvailable == null ? 0L : stockAvailable));
        target.put("status_code", "ON_SHELF".equalsIgnoreCase(saleStatusCode) ? "ON_SHELF" : "OFF_SHELF");
        if (StringUtils.hasText(mainImageUrl)) {
            target.put("main_image_url", mainImageUrl);
        } else if (!target.containsKey("main_image_url")) {
            target.put("main_image_url", "");
        }
        if (detailHtml != null) {
            target.put("detail_html", String.valueOf(detailHtml));
        } else if (!target.containsKey("detail_html")) {
            target.put("detail_html", "");
        }
        target.put("limit_per_user", limitPerUser == null ? toInt(target.getOrDefault("limit_per_user", 0)) : Math.max(0, limitPerUser));
    }

    public synchronized void upsertProductSkusFromAdmin(Long productId,
                                                        String skcCode,
                                                        List<Map<String, Object>> adminSkus) {
        if (productId == null || productId <= 0) return;
        Map<String, Object> target = products.computeIfAbsent(productId, key -> new HashMap<>());
        if (StringUtils.hasText(skcCode)) {
            target.put("skc_code", skcCode);
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        if (adminSkus != null) {
            for (Map<String, Object> item : adminSkus) {
                if (item == null) continue;
                Long skuId = toLong(item.getOrDefault("id", item.getOrDefault("sku_id", 0)));
                if (skuId == null || skuId <= 0) continue;
                Map<String, Object> sku = new HashMap<>();
                sku.put("sku_id", skuId);
                sku.put("sku_code", String.valueOf(item.getOrDefault("sku_code", "")));
                String skuName = String.valueOf(item.getOrDefault("sku_name", "默认规格"));
                sku.put("sku_name", StringUtils.hasText(skuName) ? skuName : "默认规格");
                String specText = String.valueOf(item.getOrDefault("spec_text", sku.get("sku_name")));
                sku.put("spec_text", StringUtils.hasText(specText) ? specText : String.valueOf(sku.get("sku_name")));
                long pointPrice = Math.max(0L, toLong(item.getOrDefault("point_price", 0)));
                long stockAvailable = Math.max(0L, toLong(item.getOrDefault("stock_available", 0)));
                String status = String.valueOf(item.getOrDefault("status_code", "ENABLED"));
                sku.put("point_price", pointPrice);
                sku.put("stock_available", stockAvailable);
                sku.put("status_code", "ENABLED".equalsIgnoreCase(status) ? "ENABLED" : "DISABLED");
                normalized.add(sku);
            }
        }

        normalized.sort(Comparator.comparing(item -> toLong(item.getOrDefault("sku_id", 0))));
        target.put("sku_list", normalized);

        if (!normalized.isEmpty()) {
            List<Map<String, Object>> enabled = normalized.stream()
                    .filter(item -> "ENABLED".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ENABLED"))))
                    .collect(Collectors.toList());

            List<Map<String, Object>> source = enabled.isEmpty() ? normalized : enabled;
            long minPrice = source.stream().mapToLong(item -> toLong(item.getOrDefault("point_price", 0))).min().orElse(0L);
            long totalStock = source.stream().mapToLong(item -> toLong(item.getOrDefault("stock_available", 0))).sum();
            target.put("point_price", minPrice);
            target.put("stock_available", Math.max(0L, totalStock));

            Map<String, Object> defaultSku = source.stream()
                    .filter(item -> toLong(item.getOrDefault("stock_available", 0)) > 0)
                    .findFirst()
                    .orElse(source.get(0));
            target.put("default_sku_id", toLong(defaultSku.getOrDefault("sku_id", 0)));
        }
    }

    public synchronized void removeProductById(Long productId) {
        if (productId == null) return;
        products.remove(productId);
    }

    public synchronized void updateProductMainImage(Long productId, String imageUrl) {
        if (productId == null || !StringUtils.hasText(imageUrl)) return;
        Map<String, Object> product = products.get(productId);
        if (product == null) return;
        product.put("main_image_url", imageUrl);
    }

    public synchronized void updateProductCategory(Long productId, Long categoryId) {
        if (productId == null || categoryId == null || categoryId <= 0) return;
        updateProductCategories(productId, List.of(categoryId));
    }

    public synchronized void updateProductCategories(Long productId, List<Long> categoryIds) {
        if (productId == null || productId <= 0) return;
        Map<String, Object> product = products.get(productId);
        if (product == null) return;
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (categoryIds != null) {
            for (Long one : categoryIds) {
                if (one != null && one > 0) ids.add(one);
            }
        }
        if (ids.isEmpty()) {
            Long current = toLong(product.getOrDefault("category_id", 0));
            if (current != null && current > 0) ids.add(current);
        }
        if (ids.isEmpty()) return;
        List<Long> idList = new ArrayList<>(ids);
        product.put("category_id", idList.get(0));
        product.put("category_ids", idList);
    }

    public List<String> getHomeBanners() {
        return homeBanners;
    }

    public synchronized void replaceHomeBannersFromAdmin(List<String> bannerUrls) {
        if (bannerUrls == null) return;
        homeBanners.clear();
        bannerUrls.stream()
                .filter(StringUtils::hasText)
                .forEach(homeBanners::add);
    }

    public synchronized void replaceAddressesFromAdmin(Long userId, List<Map<String, Object>> adminAddresses) {
        if (userId == null) return;
        LinkedHashMap<Long, Map<String, Object>> target = userAddressMap(userId);
        target.clear();
        if (adminAddresses == null) return;
        for (Map<String, Object> item : adminAddresses) {
            if (item == null) continue;
            Long id = toLong(item.get("id"));
            if (id == null || id <= 0) continue;
            Map<String, Object> address = new HashMap<>();
            address.put("id", id);
            address.put("user_id", userId);
            address.put("receiver_name", String.valueOf(item.getOrDefault("receiver_name", "")));
            address.put("receiver_phone", String.valueOf(item.getOrDefault("receiver_phone", "")));
            address.put("country_code", String.valueOf(item.getOrDefault("country_code", "CN")));
            address.put("province_name", String.valueOf(item.getOrDefault("province_name", "")));
            address.put("city_name", String.valueOf(item.getOrDefault("city_name", "")));
            address.put("district_name", String.valueOf(item.getOrDefault("district_name", "")));
            address.put("detail_address", String.valueOf(item.getOrDefault("detail_address", "")));
            address.put("is_default", toInt(item.getOrDefault("is_default", 0)));
            address.put("status_code", String.valueOf(item.getOrDefault("status_code", "ACTIVE")));
            address.put("created_at", String.valueOf(item.getOrDefault("created_at", now())));
            target.put(id, address);
        }
    }

    public synchronized void upsertUserPointFromAdmin(Long userId,
                                                      String nickName,
                                                      String phoneMasked,
                                                      String userStatusCode,
                                                      Long pointBalance,
                                                      Double totalConsumeAmount,
                                                      Double profitAmount) {
        if (userId == null || userId <= 0) return;

        Map<String, Object> user = users.computeIfAbsent(userId, key -> new HashMap<>());
        user.put("id", userId);
        user.put("user_no", String.valueOf(user.getOrDefault("user_no", "U" + userId)));
        user.put("nick_name", StringUtils.hasText(nickName) ? nickName : String.valueOf(user.getOrDefault("nick_name", "积分用户")));
        if (StringUtils.hasText(phoneMasked)) {
            user.put("phone_masked", phoneMasked);
        } else if (!user.containsKey("phone_masked")) {
            user.put("phone_masked", "");
        }
        user.put("user_status_code", StringUtils.hasText(userStatusCode) ? userStatusCode : "ACTIVE");
        user.put("created_at", String.valueOf(user.getOrDefault("created_at", now())));
        user.put("total_consume_amount", totalConsumeAmount == null ? 0D : totalConsumeAmount);
        user.put("profit_amount", profitAmount == null ? 0D : profitAmount);
        indexWechatOpenId(user);
        syncUserIdSeq();

        Map<String, Object> account = pointAccounts.computeIfAbsent(userId, key -> new HashMap<>());
        account.put("id", toLong(account.getOrDefault("id", userId)));
        account.put("user_id", userId);
        long balance = Math.max(0L, pointBalance == null ? 0L : pointBalance);
        account.put("point_balance", balance);
        account.put("point_total_income", toLong(account.getOrDefault("point_total_income", 0)));
        account.put("point_total_expense", toLong(account.getOrDefault("point_total_expense", 0)));
        account.put("account_status_code", "ACTIVE");
        pointLedgers.computeIfAbsent(userId, key -> new ArrayList<>());
    }

    public synchronized Map<String, Object> findOrCreateWechatUser(String openId, String unionId) {
        if (!StringUtils.hasText(openId)) return null;
        String normalizedOpenId = openId.trim();
        Long userId = wxOpenIdToUserId.get(normalizedOpenId);
        if (userId != null && userId > 0) {
            Map<String, Object> existing = users.get(userId);
            if (existing != null) {
                if (StringUtils.hasText(unionId)) {
                    existing.put("union_id", unionId.trim());
                }
                existing.put("open_id", normalizedOpenId);
                ensurePointAccountAndLedger(userId);
                return existing;
            }
        }

        for (Map<String, Object> user : users.values()) {
            String currentOpenId = String.valueOf(user.getOrDefault("open_id", ""));
            if (!normalizedOpenId.equals(currentOpenId)) continue;
            Long existingUserId = toLong(user.get("id"));
            if (existingUserId == null || existingUserId <= 0) continue;
            wxOpenIdToUserId.put(normalizedOpenId, existingUserId);
            if (StringUtils.hasText(unionId)) {
                user.put("union_id", unionId.trim());
            }
            ensurePointAccountAndLedger(existingUserId);
            return user;
        }

        long newUserId = userIdSeq.incrementAndGet();
        Map<String, Object> user = new HashMap<>();
        user.put("id", newUserId);
        user.put("user_no", "U" + newUserId);
        user.put("nick_name", "微信用户" + newUserId);
        user.put("avatar_url", "https://dummyimage.com/128x128/e8ebf1/7f8796&text=U");
        user.put("phone", "");
        user.put("phone_masked", "");
        user.put("user_status_code", "ACTIVE");
        user.put("created_at", now());
        user.put("open_id", normalizedOpenId);
        if (StringUtils.hasText(unionId)) {
            user.put("union_id", unionId.trim());
        }
        users.put(newUserId, user);
        wxOpenIdToUserId.put(normalizedOpenId, newUserId);
        ensurePointAccountAndLedger(newUserId);
        return user;
    }

    public synchronized void bindWechatUserPhone(Long userId, String phone) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(phone)) return;
        String normalizedPhone = phone.trim();
        String maskedPhone = maskPhone(normalizedPhone);
        for (Map<String, Object> existed : users.values()) {
            if (existed == null) continue;
            Long existedUserId = toLong(existed.get("id"));
            if (Objects.equals(existedUserId, userId)) continue;
            String existedPhone = String.valueOf(existed.getOrDefault("phone", "")).trim();
            String existedMasked = String.valueOf(existed.getOrDefault("phone_masked", "")).trim();
            if (!normalizedPhone.equals(existedPhone) && !maskedPhone.equals(existedMasked)) continue;
            existed.put("phone", "");
            existed.put("phone_masked", "");
        }
        Map<String, Object> user = users.computeIfAbsent(userId, key -> new HashMap<>());
        user.put("id", userId);
        user.put("user_no", String.valueOf(user.getOrDefault("user_no", "U" + userId)));
        user.put("nick_name", String.valueOf(user.getOrDefault("nick_name", "微信用户" + userId)));
        user.put("user_status_code", String.valueOf(user.getOrDefault("user_status_code", "ACTIVE")));
        user.put("created_at", String.valueOf(user.getOrDefault("created_at", now())));
        user.put("phone", normalizedPhone);
        user.put("phone_masked", maskedPhone);
        indexWechatOpenId(user);
        ensurePointAccountAndLedger(userId);
        syncUserIdSeq();
    }

    public synchronized void syncWechatIdentityFromAdmin(Long userId,
                                                         String openId,
                                                         String unionId,
                                                         String phone,
                                                         String phoneMasked,
                                                         String avatarUrl) {
        if (userId == null || userId <= 0) return;
        Map<String, Object> user = users.computeIfAbsent(userId, key -> new HashMap<>());
        user.put("id", userId);
        user.put("user_no", String.valueOf(user.getOrDefault("user_no", "U" + userId)));
        user.put("nick_name", String.valueOf(user.getOrDefault("nick_name", "微信用户" + userId)));
        user.put("user_status_code", String.valueOf(user.getOrDefault("user_status_code", "ACTIVE")));
        user.put("created_at", String.valueOf(user.getOrDefault("created_at", now())));

        String safeOpenId = String.valueOf(openId == null ? "" : openId).trim();
        if (StringUtils.hasText(safeOpenId)) {
            user.put("open_id", safeOpenId);
            wxOpenIdToUserId.put(safeOpenId, userId);
        }
        String safeUnionId = String.valueOf(unionId == null ? "" : unionId).trim();
        if (StringUtils.hasText(safeUnionId)) {
            user.put("union_id", safeUnionId);
        }
        String safePhone = String.valueOf(phone == null ? "" : phone).trim();
        if (StringUtils.hasText(safePhone)) {
            user.put("phone", safePhone);
            user.put("phone_masked", maskPhone(safePhone));
        } else {
            String safeMasked = String.valueOf(phoneMasked == null ? "" : phoneMasked).trim();
            if (StringUtils.hasText(safeMasked)) {
                user.put("phone_masked", safeMasked);
            }
        }
        String safeAvatar = String.valueOf(avatarUrl == null ? "" : avatarUrl).trim();
        if (StringUtils.hasText(safeAvatar)) {
            user.put("avatar_url", safeAvatar);
        }

        indexWechatOpenId(user);
        ensurePointAccountAndLedger(userId);
        syncUserIdSeq();
    }

    private void ensurePointAccountAndLedger(Long userId) {
        if (userId == null || userId <= 0) return;
        Map<String, Object> account = pointAccounts.computeIfAbsent(userId, key -> new HashMap<>());
        account.put("id", toLong(account.getOrDefault("id", userId)));
        account.put("user_id", userId);
        account.put("point_balance", toLong(account.getOrDefault("point_balance", 0)));
        account.put("point_total_income", toLong(account.getOrDefault("point_total_income", 0)));
        account.put("point_total_expense", toLong(account.getOrDefault("point_total_expense", 0)));
        account.put("account_status_code", String.valueOf(account.getOrDefault("account_status_code", "ACTIVE")));
        pointLedgers.computeIfAbsent(userId, key -> new ArrayList<>());
    }

    private void indexWechatOpenId(Map<String, Object> user) {
        if (user == null) return;
        String openId = String.valueOf(user.getOrDefault("open_id", "")).trim();
        Long userId = toLong(user.get("id"));
        if (!StringUtils.hasText(openId) || userId == null || userId <= 0) return;
        wxOpenIdToUserId.put(openId, userId);
    }

    private void syncUserIdSeq() {
        long maxUserId = users.keySet().stream()
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(1000L);
        userIdSeq.updateAndGet(current -> Math.max(current, maxUserId));
    }

    public synchronized Map<Long, Long> normalizeDuplicateWechatUsers() {
        if (users.size() <= 1) return new LinkedHashMap<>();

        Map<String, Long> keyOwner = new LinkedHashMap<>();
        Map<Long, Long> rawMapping = new LinkedHashMap<>();
        List<Long> userIds = users.keySet().stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        for (Long userId : userIds) {
            Map<String, Object> user = users.get(userId);
            if (user == null) continue;
            List<String> keys = identityKeys(user);
            if (keys.isEmpty()) continue;

            Long canonical = userId;
            for (String key : keys) {
                Long owner = keyOwner.get(key);
                if (owner == null || owner <= 0) continue;
                canonical = Math.min(canonical, resolveFinalUserId(owner, rawMapping));
            }

            Long finalCanonical = resolveFinalUserId(canonical, rawMapping);
            if (!Objects.equals(userId, finalCanonical)) {
                rawMapping.put(userId, finalCanonical);
            }
            for (String key : keys) {
                Long owner = keyOwner.get(key);
                if (owner == null || owner <= 0) {
                    keyOwner.put(key, finalCanonical);
                    continue;
                }
                Long ownerFinal = resolveFinalUserId(owner, rawMapping);
                Long mergedCanonical = Math.min(ownerFinal, finalCanonical);
                if (!Objects.equals(ownerFinal, mergedCanonical)) {
                    rawMapping.put(ownerFinal, mergedCanonical);
                }
                if (!Objects.equals(finalCanonical, mergedCanonical)) {
                    rawMapping.put(finalCanonical, mergedCanonical);
                }
                finalCanonical = mergedCanonical;
                keyOwner.put(key, mergedCanonical);
            }
        }

        Map<Long, Long> mapping = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : rawMapping.entrySet()) {
            Long from = entry.getKey();
            Long to = resolveFinalUserId(entry.getValue(), rawMapping);
            if (from == null || to == null || from <= 0 || to <= 0) continue;
            if (Objects.equals(from, to)) continue;
            mapping.put(from, to);
        }
        if (mapping.isEmpty()) return new LinkedHashMap<>();

        List<Map.Entry<Long, Long>> merges = new ArrayList<>(mapping.entrySet());
        merges.sort(Map.Entry.comparingByKey(Comparator.reverseOrder()));
        for (Map.Entry<Long, Long> merge : merges) {
            Long fromUserId = merge.getKey();
            Long toUserId = resolveFinalUserId(merge.getValue(), mapping);
            if (fromUserId == null || toUserId == null || Objects.equals(fromUserId, toUserId)) continue;
            mergeUserData(fromUserId, toUserId);
        }

        Map<Long, Long> compact = new LinkedHashMap<>();
        mapping.forEach((from, to) -> {
            Long finalTo = resolveFinalUserId(to, mapping);
            if (from != null && finalTo != null && !Objects.equals(from, finalTo)) {
                compact.put(from, finalTo);
            }
        });
        syncUserIdSeq();
        return compact;
    }

    private void mergeUserData(Long fromUserId, Long toUserId) {
        if (fromUserId == null || toUserId == null || Objects.equals(fromUserId, toUserId)) return;
        Map<String, Object> fromUser = users.get(fromUserId);
        if (fromUser == null) return;
        Map<String, Object> toUser = users.computeIfAbsent(toUserId, key -> new HashMap<>());

        fillIfBlank(toUser, "user_no", fromUser.get("user_no"));
        fillIfBlank(toUser, "nick_name", fromUser.get("nick_name"));
        fillIfBlank(toUser, "avatar_url", fromUser.get("avatar_url"));
        fillIfBlank(toUser, "open_id", fromUser.get("open_id"));
        fillIfBlank(toUser, "union_id", fromUser.get("union_id"));
        fillIfBlank(toUser, "phone", fromUser.get("phone"));
        fillIfBlank(toUser, "phone_masked", fromUser.get("phone_masked"));
        toUser.put("id", toUserId);

        String fromStatus = String.valueOf(fromUser.getOrDefault("user_status_code", "ACTIVE"));
        String toStatus = String.valueOf(toUser.getOrDefault("user_status_code", "ACTIVE"));
        if ("FROZEN".equalsIgnoreCase(fromStatus) || "FROZEN".equalsIgnoreCase(toStatus)) {
            toUser.put("user_status_code", "FROZEN");
        } else {
            toUser.put("user_status_code", "ACTIVE");
        }
        String fromCreatedAt = String.valueOf(fromUser.getOrDefault("created_at", ""));
        String toCreatedAt = String.valueOf(toUser.getOrDefault("created_at", ""));
        if (!StringUtils.hasText(toCreatedAt) || (StringUtils.hasText(fromCreatedAt) && fromCreatedAt.compareTo(toCreatedAt) < 0)) {
            toUser.put("created_at", fromCreatedAt);
        }

        Map<String, Object> toAccount = pointAccounts.computeIfAbsent(toUserId, key -> new HashMap<>());
        Map<String, Object> fromAccount = pointAccounts.remove(fromUserId);
        toAccount.put("id", toLong(toAccount.getOrDefault("id", toUserId)));
        toAccount.put("user_id", toUserId);
        toAccount.put("point_balance", toLong(toAccount.getOrDefault("point_balance", 0)) + toLong(fromAccount == null ? 0 : fromAccount.getOrDefault("point_balance", 0)));
        toAccount.put("point_total_income", toLong(toAccount.getOrDefault("point_total_income", 0)) + toLong(fromAccount == null ? 0 : fromAccount.getOrDefault("point_total_income", 0)));
        toAccount.put("point_total_expense", toLong(toAccount.getOrDefault("point_total_expense", 0)) + toLong(fromAccount == null ? 0 : fromAccount.getOrDefault("point_total_expense", 0)));
        toAccount.put("account_status_code", String.valueOf(toAccount.getOrDefault("account_status_code", "ACTIVE")));

        List<Map<String, Object>> toLedgers = pointLedgers.computeIfAbsent(toUserId, key -> new ArrayList<>());
        List<Map<String, Object>> fromLedgers = pointLedgers.remove(fromUserId);
        if (fromLedgers != null && !fromLedgers.isEmpty()) {
            fromLedgers.forEach(item -> item.put("user_id", toUserId));
            toLedgers.addAll(fromLedgers);
            toLedgers.sort((a, b) -> {
                String timeA = String.valueOf(a.getOrDefault("occurred_at", ""));
                String timeB = String.valueOf(b.getOrDefault("occurred_at", ""));
                int cmp = timeB.compareTo(timeA);
                if (cmp != 0) return cmp;
                return Long.compare(toLong(b.getOrDefault("id", 0)), toLong(a.getOrDefault("id", 0)));
            });
        }

        LinkedHashMap<Long, Map<String, Object>> toAddresses = addresses.computeIfAbsent(toUserId, key -> new LinkedHashMap<>());
        LinkedHashMap<Long, Map<String, Object>> fromAddresses = addresses.remove(fromUserId);
        if (fromAddresses != null && !fromAddresses.isEmpty()) {
            fromAddresses.values().forEach(item -> {
                Long addressId = toLong(item.getOrDefault("id", 0));
                if (addressId <= 0 || toAddresses.containsKey(addressId)) {
                    addressId = addressIdSeq.incrementAndGet();
                    item.put("id", addressId);
                }
                item.put("user_id", toUserId);
                toAddresses.put(addressId, item);
            });
        }

        orders.values().forEach(item -> {
            if (Objects.equals(toLong(item.getOrDefault("user_id", 0)), fromUserId)) {
                item.put("user_id", toUserId);
            }
        });

        assets.values().forEach(item -> {
            if (Objects.equals(toLong(item.getOrDefault("user_id", 0)), fromUserId)) {
                item.put("user_id", toUserId);
            }
        });

        wxOpenIdToUserId.replaceAll((k, v) -> Objects.equals(v, fromUserId) ? toUserId : v);
        tokenToUserId.replaceAll((k, v) -> Objects.equals(v, fromUserId) ? toUserId : v);
        refreshToUserId.replaceAll((k, v) -> Objects.equals(v, fromUserId) ? toUserId : v);

        users.remove(fromUserId);
    }

    private List<String> identityKeys(Map<String, Object> user) {
        List<String> keys = new ArrayList<>();
        if (user == null) return keys;
        String openId = String.valueOf(user.getOrDefault("open_id", "")).trim();
        if (StringUtils.hasText(openId)) keys.add("open:" + openId);
        String unionId = String.valueOf(user.getOrDefault("union_id", "")).trim();
        if (StringUtils.hasText(unionId)) keys.add("union:" + unionId);
        String phone = normalizePhoneDigits(String.valueOf(user.getOrDefault("phone", "")));
        if (StringUtils.hasText(phone)) keys.add("phone:" + phone);
        String phoneMasked = String.valueOf(user.getOrDefault("phone_masked", "")).trim();
        if (StringUtils.hasText(phoneMasked)) keys.add("masked:" + phoneMasked);
        return keys;
    }

    private String normalizePhoneDigits(String raw) {
        String text = String.valueOf(raw == null ? "" : raw).trim();
        if (!StringUtils.hasText(text)) return "";
        return text.replaceAll("\\D", "");
    }

    private Long resolveFinalUserId(Long userId, Map<Long, Long> mapping) {
        if (userId == null || mapping == null || mapping.isEmpty()) return userId;
        Long cursor = userId;
        Set<Long> guard = new HashSet<>();
        while (cursor != null && mapping.containsKey(cursor) && guard.add(cursor)) {
            cursor = mapping.get(cursor);
        }
        return cursor == null ? userId : cursor;
    }

    private void fillIfBlank(Map<String, Object> target, String key, Object value) {
        if (target == null || !StringUtils.hasText(key)) return;
        String current = String.valueOf(target.getOrDefault(key, "")).trim();
        if (StringUtils.hasText(current)) return;
        String next = String.valueOf(value == null ? "" : value).trim();
        if (!StringUtils.hasText(next)) return;
        target.put(key, next);
    }

    public synchronized void replacePointLedgersFromAdmin(Long userId, List<Map<String, Object>> adminLedgers) {
        if (userId == null || userId <= 0) return;
        List<Map<String, Object>> mapped = new ArrayList<>();
        long maxId = 0L;

        if (adminLedgers != null) {
            List<Map<String, Object>> sorted = adminLedgers.stream()
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> {
                        String timeA = String.valueOf(a.getOrDefault("occurred_at", ""));
                        String timeB = String.valueOf(b.getOrDefault("occurred_at", ""));
                        int cmp = timeB.compareTo(timeA);
                        if (cmp != 0) return cmp;
                        return Long.compare(toLong(b.get("id")), toLong(a.get("id")));
                    })
                    .collect(Collectors.toList());

            for (Map<String, Object> item : sorted) {
                Long id = toLong(item.get("id"));
                if (id == null || id <= 0) {
                    id = ledgerIdSeq.incrementAndGet();
                }
                maxId = Math.max(maxId, id);

                Map<String, Object> ledger = new HashMap<>();
                ledger.put("id", id);
                ledger.put("user_id", userId);
                ledger.put("biz_type_code", String.valueOf(item.getOrDefault("biz_type_code", "")));
                ledger.put("change_amount", toLong(item.getOrDefault("change_amount", 0)));
                ledger.put("balance_after", toLong(item.getOrDefault("balance_after", 0)));
                ledger.put("occurred_at", String.valueOf(item.getOrDefault("occurred_at", now())));
                ledger.put("remark", String.valueOf(item.getOrDefault("note", item.getOrDefault("remark", ""))));
                ledger.put("consume_amount", item.getOrDefault("consume_amount", 0D));
                ledger.put("profit_change", item.getOrDefault("profit_change", 0D));
                ledger.put("prize_name", String.valueOf(item.getOrDefault("prize_name", "")));
                ledger.put("draw_count", toLong(item.getOrDefault("draw_count", 0)));
                mapped.add(ledger);
            }
        }

        pointLedgers.put(userId, mapped);
        if (maxId > 0) {
            ledgerIdSeq.set(Math.max(ledgerIdSeq.get(), maxId));
        }
    }

    public synchronized void upsertOrderFromAdmin(Map<String, Object> adminOrder,
                                                  List<Map<String, Object>> adminItems,
                                                  List<Map<String, Object>> adminFlows,
                                                  Map<String, Object> adminDelivery,
                                                  Map<String, Object> adminAddressSnapshot) {
        if (adminOrder == null) return;
        Long orderId = toLong(adminOrder.get("id"));
        if (orderId == null || orderId <= 0) return;
        Long userId = toLong(adminOrder.getOrDefault("user_id", 0));
        if (userId == null || userId <= 0) return;

        Map<String, Object> order = orders.computeIfAbsent(orderId, key -> new HashMap<>());
        order.put("id", orderId);
        order.put("order_no", String.valueOf(adminOrder.getOrDefault("order_no", "")));
        order.put("user_id", userId);
        order.put("order_type_code", "EXCHANGE");
        order.put("order_status_code", String.valueOf(adminOrder.getOrDefault("order_status_code", "PENDING_AUDIT")));
        order.put("total_point_amount", toLong(adminOrder.getOrDefault("total_point_amount", 0)));
        order.put("total_item_count", adminItems == null ? 1 : Math.max(1, adminItems.stream().mapToInt(item -> toInt(item.getOrDefault("quantity", 1))).sum()));
        order.put("user_remark", String.valueOf(adminOrder.getOrDefault("remark", "")));
        order.put("admin_remark", String.valueOf(adminOrder.getOrDefault("admin_remark", "")));
        order.put("reject_reason", String.valueOf(adminOrder.getOrDefault("reject_reason", "")));
        order.put("buyer_decision_required", Boolean.parseBoolean(String.valueOf(adminOrder.getOrDefault("buyer_decision_required", false))));
        order.put("point_refunded", Boolean.parseBoolean(String.valueOf(adminOrder.getOrDefault("point_refunded", false))));
        order.put("procurement_status", String.valueOf(adminOrder.getOrDefault("procurement_status", "PENDING_PROCURE")));
        order.put("procured_at", String.valueOf(adminOrder.getOrDefault("procured_at", "")));
        order.put("procured_by", String.valueOf(adminOrder.getOrDefault("procured_by", "")));
        String submitAt = String.valueOf(adminOrder.getOrDefault("submit_at", now()));
        order.put("submit_at", submitAt);
        order.put("created_at", submitAt);

        String firstProductName = "";
        if (adminItems != null && !adminItems.isEmpty()) {
            Map<String, Object> first = adminItems.get(0);
            firstProductName = String.valueOf(first.getOrDefault("spu_name", first.getOrDefault("sku_name", "")));
        }
        if (!StringUtils.hasText(firstProductName)) {
            firstProductName = String.valueOf(order.getOrDefault("product_name_snapshot", ""));
        }
        order.put("product_name_snapshot", firstProductName);
        String firstMainImage = String.valueOf(adminOrder.getOrDefault("main_image_snapshot", adminOrder.getOrDefault("main_image_url", "")));

        List<Map<String, Object>> mappedItems = new ArrayList<>();
        if (adminItems != null) {
            for (Map<String, Object> item : adminItems) {
                Map<String, Object> mapped = new HashMap<>();
                Long itemId = toLong(item.getOrDefault("id", orderId * 10 + mappedItems.size() + 1));
                Long spuId = toLong(item.getOrDefault("spu_id", item.getOrDefault("product_id", 0)));
                String productName = String.valueOf(item.getOrDefault("spu_name", item.getOrDefault("sku_name", "")));
                String mainImage = String.valueOf(item.getOrDefault("main_image_snapshot", item.getOrDefault("main_image_url", "")));
                if (!StringUtils.hasText(mainImage)) {
                    mainImage = resolveProductMainImage(spuId, productName);
                }
                mapped.put("id", itemId);
                mapped.put("order_id", orderId);
                mapped.put("order_no", String.valueOf(order.getOrDefault("order_no", "")));
                mapped.put("spu_id", spuId);
                mapped.put("product_name_snapshot", productName);
                mapped.put("main_image_snapshot", mainImage);
                mapped.put("unit_point_price", toLong(item.getOrDefault("point_price", 0)));
                mapped.put("quantity", toInt(item.getOrDefault("quantity", 1)));
                mapped.put("total_point_amount", toLong(item.getOrDefault("total_point_amount", 0)));
                mappedItems.add(mapped);
                if (!StringUtils.hasText(firstMainImage)) {
                    firstMainImage = mainImage;
                }
            }
        }
        order.put("main_image_snapshot", firstMainImage);
        orderItems.put(orderId, deduplicateOrderItems(mappedItems));

        List<Map<String, Object>> mappedFlows = new ArrayList<>();
        if (adminFlows != null) {
            for (Map<String, Object> flow : adminFlows) {
                Map<String, Object> mapped = new HashMap<>();
                Long flowId = toLong(flow.getOrDefault("id", orderId * 100 + mappedFlows.size() + 1));
                mapped.put("id", flowId);
                mapped.put("order_id", orderId);
                mapped.put("from_status_code", String.valueOf(flow.getOrDefault("from_status", "")));
                mapped.put("to_status_code", String.valueOf(flow.getOrDefault("to_status", "")));
                mapped.put("action_code", String.valueOf(flow.getOrDefault("action_text", "")));
                mapped.put("operated_at", String.valueOf(flow.getOrDefault("occurred_at", now())));
                mapped.put("remark", String.valueOf(flow.getOrDefault("note", "")));
                mappedFlows.add(mapped);
            }
        }
        orderFlows.put(orderId, mappedFlows);

        Map<String, Object> delivery = new HashMap<>();
        if (adminDelivery != null) {
            delivery.put("order_id", orderId);
            delivery.put("company_name", String.valueOf(adminDelivery.getOrDefault("express_company", "")));
            delivery.put("tracking_no", String.valueOf(adminDelivery.getOrDefault("express_no", "")));
            delivery.put("shipper_code", String.valueOf(adminDelivery.getOrDefault("shipper_code", "")));
            delivery.put("delivery_status_code", String.valueOf(adminDelivery.getOrDefault("delivery_status_code", "")));
            delivery.put("delivery_status_text", String.valueOf(adminDelivery.getOrDefault("delivery_status_text", "")));
            delivery.put("ship_at", String.valueOf(adminDelivery.getOrDefault("ship_at", "")));
            delivery.put("signed_at", String.valueOf(adminDelivery.getOrDefault("signed_at", "")));
            delivery.put("latest_trace_time", String.valueOf(adminDelivery.getOrDefault("latest_trace_time", "")));
            delivery.put("latest_trace_station", String.valueOf(adminDelivery.getOrDefault("latest_trace_station", "")));
            orderDeliveries.put(orderId, delivery);
        } else {
            orderDeliveries.remove(orderId);
        }

        Map<String, Object> addressSnapshot = new HashMap<>();
        if (adminAddressSnapshot != null) {
            addressSnapshot.put("order_id", orderId);
            addressSnapshot.put("order_no", String.valueOf(order.getOrDefault("order_no", "")));
            addressSnapshot.put("receiver_name", String.valueOf(adminAddressSnapshot.getOrDefault("receiver_name", "")));
            addressSnapshot.put("receiver_phone", String.valueOf(adminAddressSnapshot.getOrDefault("receiver_phone", "")));
            addressSnapshot.put("province_name", String.valueOf(adminAddressSnapshot.getOrDefault("province_name", "")));
            addressSnapshot.put("city_name", String.valueOf(adminAddressSnapshot.getOrDefault("city_name", "")));
            addressSnapshot.put("district_name", String.valueOf(adminAddressSnapshot.getOrDefault("district_name", "")));
            addressSnapshot.put("detail_address", String.valueOf(adminAddressSnapshot.getOrDefault("detail_address", "")));
            orderAddressSnapshots.put(orderId, addressSnapshot);
        } else if (adminDelivery != null) {
            addressSnapshot.put("order_id", orderId);
            addressSnapshot.put("order_no", String.valueOf(order.getOrDefault("order_no", "")));
            addressSnapshot.put("receiver_name", String.valueOf(adminDelivery.getOrDefault("receiver_name", "")));
            addressSnapshot.put("receiver_phone", String.valueOf(adminDelivery.getOrDefault("receiver_phone", "")));
            addressSnapshot.put("province_name", "");
            addressSnapshot.put("city_name", "");
            addressSnapshot.put("district_name", "");
            addressSnapshot.put("detail_address", String.valueOf(adminDelivery.getOrDefault("receiver_address", "")));
            orderAddressSnapshots.put(orderId, addressSnapshot);
        }

        ensureOrderSequences(orderId, parseOrderNoNumeric(String.valueOf(order.getOrDefault("order_no", ""))));
    }

    public synchronized void removeOrderById(Long orderId) {
        if (orderId == null) return;
        orders.remove(orderId);
        orderItems.remove(orderId);
        orderFlows.remove(orderId);
        orderDeliveries.remove(orderId);
        orderAddressSnapshots.remove(orderId);
        idempotentOrderMap.entrySet().removeIf(entry -> Objects.equals(entry.getValue(), orderId));
    }

    public synchronized void ensureOrderSequences(long minOrderId, long minOrderNoNumeric) {
        if (minOrderId > 0) {
            orderIdSeq.set(Math.max(orderIdSeq.get(), minOrderId));
        }
        if (minOrderNoNumeric > 0) {
            orderNoSeq.set(Math.max(orderNoSeq.get(), minOrderNoNumeric));
        }
    }

    private long parseOrderNoNumeric(String orderNo) {
        if (!StringUtils.hasText(orderNo)) return 0L;
        String text = orderNo.trim();
        if (text.length() >= 2 && (text.startsWith("EO") || text.startsWith("eo"))) {
            text = text.substring(2);
        }
        text = text.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(text)) return 0L;
        if (text.length() > 8) {
            String maybeDate = text.substring(0, 8);
            if (looksLikeDatePrefix(maybeDate)) {
                text = text.substring(8);
            }
        }
        try {
            return Long.parseLong(text);
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private boolean looksLikeDatePrefix(String text) {
        if (!StringUtils.hasText(text) || text.length() != 8) return false;
        try {
            int year = Integer.parseInt(text.substring(0, 4));
            int month = Integer.parseInt(text.substring(4, 6));
            int day = Integer.parseInt(text.substring(6, 8));
            if (year < 2000 || year > 2199) return false;
            return month >= 1 && month <= 12 && day >= 1 && day <= 31;
        } catch (Exception ex) {
            return false;
        }
    }

    private String generateOrderNo() {
        long seq = orderNoSeq.incrementAndGet();
        String datePrefix = LocalDate.now(BEIJING_ZONE).format(DAY);
        return datePrefix + String.format("%06d", seq);
    }

    public LinkedHashMap<Long, Map<String, Object>> userAddressMap(Long userId) {
        return addresses.computeIfAbsent(userId, k -> new LinkedHashMap<>());
    }

    public synchronized Map<String, Object> createAddress(Long userId, Map<String, Object> payload) {
        LinkedHashMap<Long, Map<String, Object>> map = userAddressMap(userId);
        Long id = addressIdSeq.incrementAndGet();
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("user_id", userId);
        item.put("receiver_name", payload.getOrDefault("receiver_name", ""));
        item.put("receiver_phone", payload.getOrDefault("receiver_phone", ""));
        item.put("country_code", payload.getOrDefault("country_code", "CN"));
        item.put("province_name", payload.getOrDefault("province_name", ""));
        item.put("city_name", payload.getOrDefault("city_name", ""));
        item.put("district_name", payload.getOrDefault("district_name", ""));
        item.put("detail_address", payload.getOrDefault("detail_address", ""));
        item.put("is_default", toInt(payload.getOrDefault("is_default", 0)));
        item.put("status_code", payload.getOrDefault("status_code", "ACTIVE"));
        item.put("created_at", now());
        if (toInt(item.get("is_default")) == 1) clearDefaultAddress(userId);
        map.put(id, item);
        return item;
    }

    public synchronized Map<String, Object> updateAddress(Long userId, Long addressId, Map<String, Object> payload) {
        Map<String, Object> address = userAddressMap(userId).get(addressId);
        if (address == null) return null;
        address.put("receiver_name", payload.getOrDefault("receiver_name", address.get("receiver_name")));
        address.put("receiver_phone", payload.getOrDefault("receiver_phone", address.get("receiver_phone")));
        address.put("province_name", payload.getOrDefault("province_name", address.get("province_name")));
        address.put("city_name", payload.getOrDefault("city_name", address.get("city_name")));
        address.put("district_name", payload.getOrDefault("district_name", address.get("district_name")));
        address.put("detail_address", payload.getOrDefault("detail_address", address.get("detail_address")));
        int isDefault = toInt(payload.getOrDefault("is_default", address.get("is_default")));
        if (isDefault == 1) clearDefaultAddress(userId);
        address.put("is_default", isDefault);
        return address;
    }

    public synchronized boolean removeAddress(Long userId, Long addressId) {
        return userAddressMap(userId).remove(addressId) != null;
    }

    public synchronized boolean setDefaultAddress(Long userId, Long addressId) {
        Map<String, Object> target = userAddressMap(userId).get(addressId);
        if (target == null) return false;
        clearDefaultAddress(userId);
        target.put("is_default", 1);
        return true;
    }

    private void clearDefaultAddress(Long userId) {
        userAddressMap(userId).values().forEach(item -> item.put("is_default", 0));
    }

    public synchronized Map<String, Object> createOrder(Long userId, Long spuId, Long addressId, String requestId, String userRemark) {
        return createOrder(userId, spuId, null, addressId, requestId, userRemark);
    }

    @SuppressWarnings("unchecked")
    public synchronized Map<String, Object> createOrder(Long userId, Long spuId, Long skuId, Long addressId, String requestId, String userRemark) {
        Map<String, Object> product = products.get(spuId);
        if (product == null) return null;

        Long existedOrderId = idempotentOrderMap.get(requestId);
        if (existedOrderId != null) {
            Map<String, Object> order = orders.get(existedOrderId);
            if (order != null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("order_id", existedOrderId);
                resp.put("order_no", order.get("order_no"));
                resp.put("idempotent_hit", true);
                return resp;
            }
        }

        long pointPrice = toLong(product.get("point_price"));
        long stock = toLong(product.get("stock_available"));
        Map<String, Object> pointAccount = pointAccounts.get(userId);
        long balance = toLong(pointAccount.get("point_balance"));

        List<Map<String, Object>> skuList = new ArrayList<>();
        Object rawSkuList = product.get("sku_list");
        if (rawSkuList instanceof List<?>) {
            for (Object item : (List<?>) rawSkuList) {
                if (item instanceof Map) {
                    skuList.add(new HashMap<>((Map<String, Object>) item));
                }
            }
        }

        Map<String, Object> selectedSku = null;
        if (skuId != null && skuId > 0) {
            selectedSku = skuList.stream()
                    .filter(item -> Objects.equals(toLong(item.getOrDefault("sku_id", item.get("id"))), skuId))
                    .findFirst()
                    .orElse(null);
            if (selectedSku == null) return null;
        } else if (!skuList.isEmpty()) {
            Long defaultSkuId = toLong(product.getOrDefault("default_sku_id", 0));
            if (defaultSkuId != null && defaultSkuId > 0) {
                selectedSku = skuList.stream()
                        .filter(item -> Objects.equals(toLong(item.getOrDefault("sku_id", item.get("id"))), defaultSkuId))
                        .findFirst()
                        .orElse(null);
            }
            if (selectedSku == null) {
                selectedSku = skuList.stream()
                        .filter(item -> "ENABLED".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ENABLED"))))
                        .filter(item -> toLong(item.getOrDefault("stock_available", 0)) > 0)
                        .findFirst()
                        .orElseGet(() -> skuList.stream()
                                .filter(item -> "ENABLED".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ENABLED"))))
                                .findFirst()
                                .orElse(skuList.get(0)));
            }
        }

        Long selectedSkuId = null;
        String selectedSkuName = "";
        long selectedSkuStock = Long.MAX_VALUE;
        if (selectedSku != null) {
            if (!"ENABLED".equalsIgnoreCase(String.valueOf(selectedSku.getOrDefault("status_code", "ENABLED")))) {
                return null;
            }
            selectedSkuId = toLong(selectedSku.getOrDefault("sku_id", selectedSku.get("id")));
            selectedSkuName = String.valueOf(selectedSku.getOrDefault("sku_name", "默认规格"));
            pointPrice = toLong(selectedSku.getOrDefault("point_price", pointPrice));
            selectedSkuStock = toLong(selectedSku.getOrDefault("stock_available", 0));
        }

        if (stock <= 0 || balance < pointPrice || selectedSkuStock <= 0) return null;

        String productType = String.valueOf(product.getOrDefault("product_type_code", "VIRTUAL"));
        if ("PHYSICAL".equalsIgnoreCase(productType) && addressId == null) return null;

        long orderId = orderIdSeq.incrementAndGet();
        String orderNo = generateOrderNo();
        String now = now();

        Map<String, Object> order = new HashMap<>();
        order.put("id", orderId);
        order.put("order_no", orderNo);
        order.put("user_id", userId);
        order.put("order_type_code", "EXCHANGE");
        order.put("product_type_code", productType);
        order.put("order_status_code", "PENDING_AUDIT");
        order.put("delivery_status_code", "PENDING");
        order.put("total_point_amount", pointPrice);
        order.put("total_item_count", 1);
        order.put("user_remark", userRemark == null ? "" : userRemark);
        order.put("submit_at", now);
        order.put("created_at", now);
        order.put("main_image_snapshot", product.get("main_image_url"));
        order.put("product_name_snapshot", product.get("product_name"));
        order.put("sku_id", selectedSkuId);
        order.put("sku_name_snapshot", selectedSkuName);
        order.put("buyer_decision_required", false);
        order.put("point_refunded", false);
        order.put("procurement_status", "PENDING_PROCURE");
        order.put("procured_at", "");
        order.put("procured_by", "");
        orders.put(orderId, order);

        Map<String, Object> item = new HashMap<>();
        item.put("id", orderId * 10);
        item.put("order_id", orderId);
        item.put("order_no", orderNo);
        item.put("spu_id", spuId);
        item.put("sku_id", selectedSkuId);
        item.put("sku_name_snapshot", selectedSkuName);
        item.put("product_name_snapshot", product.get("product_name"));
        item.put("main_image_snapshot", product.get("main_image_url"));
        item.put("unit_point_price", pointPrice);
        item.put("quantity", 1);
        item.put("total_point_amount", pointPrice);
        orderItems.put(orderId, new ArrayList<>(List.of(item)));

        Map<String, Object> flow = new HashMap<>();
        flow.put("id", orderId * 100);
        flow.put("order_id", orderId);
        flow.put("from_status_code", "INIT");
        flow.put("to_status_code", "PENDING_AUDIT");
        flow.put("action_code", "SUBMIT");
        flow.put("operated_at", now);
        flow.put("remark", "用户提交兑换");
        orderFlows.put(orderId, new ArrayList<>(List.of(flow)));

        if ("PHYSICAL".equalsIgnoreCase(productType) && addressId != null) {
            Map<String, Object> addr = userAddressMap(userId).get(addressId);
            if (addr != null) {
                Map<String, Object> snapshot = new HashMap<>(addr);
                snapshot.put("order_id", orderId);
                snapshot.put("order_no", orderNo);
                orderAddressSnapshots.put(orderId, snapshot);

                Map<String, Object> delivery = new HashMap<>();
                delivery.put("order_id", orderId);
                delivery.put("company_name", "");
                delivery.put("tracking_no", "");
                orderDeliveries.put(orderId, delivery);
            }
        }

        product.put("stock_available", stock - 1);
        if (selectedSku != null && selectedSkuId != null) {
            for (Map<String, Object> sku : skuList) {
                Long currentSkuId = toLong(sku.getOrDefault("sku_id", sku.get("id")));
                if (!Objects.equals(currentSkuId, selectedSkuId)) continue;
                long currentStock = toLong(sku.getOrDefault("stock_available", 0));
                sku.put("stock_available", Math.max(0L, currentStock - 1));
            }
            product.put("sku_list", skuList);
        }
        pointAccount.put("point_balance", balance - pointPrice);
        pointAccount.put("point_total_expense", toLong(pointAccount.get("point_total_expense")) + pointPrice);
        String orderGoodsSummary = buildOrderGoodsSummary(order, 1);
        addPointLedger(userId, "EXCHANGE_ORDER", -pointPrice, "兑换扣减：" + orderGoodsSummary);

        idempotentOrderMap.put(requestId, orderId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("order_id", orderId);
        resp.put("order_no", orderNo);
        return resp;
    }

    public synchronized boolean cancelOrder(Long userId, Long orderId) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) return false;
        if (!Objects.equals(userId, toLong(order.get("user_id")))) return false;
        String status = String.valueOf(order.get("order_status_code"));
        if (!("PENDING_AUDIT".equals(status) || "WAIT_AUDIT".equals(status))) return false;

        String now = now();
        order.put("order_status_code", "CANCELED");
        order.put("cancel_at", now);

        Map<String, Object> flow = new HashMap<>();
        flow.put("id", orderId * 100 + orderFlows.getOrDefault(orderId, new ArrayList<>()).size() + 1);
        flow.put("order_id", orderId);
        flow.put("from_status_code", status);
        flow.put("to_status_code", "CANCELED");
        flow.put("action_code", "CANCEL");
        flow.put("operated_at", now);
        flow.put("remark", "用户取消订单");
        orderFlows.computeIfAbsent(orderId, k -> new ArrayList<>()).add(flow);

        refundOrderPointIfNeeded(order, "ORDER_CANCEL_REFUND", "取消订单返还碎片");
        return true;
    }

    public synchronized boolean reviewRejectedOrder(Long userId, Long orderId, String decision, String note) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) return false;
        if (!Objects.equals(userId, toLong(order.get("user_id")))) return false;
        String status = String.valueOf(order.getOrDefault("order_status_code", ""));
        if (!"REJECTED".equalsIgnoreCase(status)) return false;

        String safeDecision = String.valueOf(decision == null ? "" : decision).trim().toUpperCase(Locale.ROOT);
        String now = now();
        if ("ACCEPT".equals(safeDecision)) {
            String rejectReason = String.valueOf(order.getOrDefault("reject_reason", ""));
            String mergedRemark = StringUtils.hasText(rejectReason)
                    ? "驳回理由：" + rejectReason
                    : "买家接受驳回理由，订单重新进入待发货";
            order.put("order_status_code", "PENDING_SHIP");
            order.put("buyer_decision_required", false);
            order.put("user_remark", mergedRemark);
            order.put("admin_remark", mergedRemark);
            Map<String, Object> flow = new HashMap<>();
            flow.put("id", orderId * 100 + orderFlows.getOrDefault(orderId, new ArrayList<>()).size() + 1);
            flow.put("order_id", orderId);
            flow.put("from_status_code", "REJECTED");
            flow.put("to_status_code", "PENDING_SHIP");
            flow.put("action_code", "USER_ACCEPT_REJECT");
            flow.put("operated_at", now);
            flow.put("remark", StringUtils.hasText(note) ? note : mergedRemark);
            orderFlows.computeIfAbsent(orderId, k -> new ArrayList<>()).add(flow);
            return true;
        }
        if ("REFUND".equals(safeDecision) || "REJECT".equals(safeDecision)) {
            order.put("order_status_code", "CLOSED");
            order.put("buyer_decision_required", false);
            order.put("close_at", now);
            refundOrderPointIfNeeded(order, "ORDER_REJECT_REFUND", "用户拒绝驳回方案，碎片已退回");
            Map<String, Object> flow = new HashMap<>();
            flow.put("id", orderId * 100 + orderFlows.getOrDefault(orderId, new ArrayList<>()).size() + 1);
            flow.put("order_id", orderId);
            flow.put("from_status_code", "REJECTED");
            flow.put("to_status_code", "CLOSED");
            flow.put("action_code", "USER_REJECT_REFUND");
            flow.put("operated_at", now);
            flow.put("remark", StringUtils.hasText(note) ? note : "用户选择退回碎片，订单已关闭");
            orderFlows.computeIfAbsent(orderId, k -> new ArrayList<>()).add(flow);
            return true;
        }
        return false;
    }

    public synchronized boolean refundOrderPointIfNeeded(Long orderId, String bizTypeCode, String remark) {
        if (orderId == null || orderId <= 0) return false;
        Map<String, Object> order = orders.get(orderId);
        if (order == null) return false;
        return refundOrderPointIfNeeded(order, bizTypeCode, remark);
    }

    private boolean refundOrderPointIfNeeded(Map<String, Object> order, String bizTypeCode, String remark) {
        if (order == null) return false;
        if (Boolean.parseBoolean(String.valueOf(order.getOrDefault("point_refunded", false)))) {
            return false;
        }
        Long userId = toLong(order.get("user_id"));
        if (userId == null || userId <= 0) return false;
        Map<String, Object> pointAccount = pointAccounts.get(userId);
        if (pointAccount == null) return false;

        long refund = Math.max(0L, toLong(order.get("total_point_amount")));
        if (refund <= 0) {
            order.put("point_refunded", true);
            return false;
        }
        pointAccount.put("point_balance", toLong(pointAccount.get("point_balance")) + refund);
        order.put("point_refunded", true);
        String reason = StringUtils.hasText(remark) ? remark : "订单返还碎片";
        String goodsSummary = buildOrderGoodsSummary(order, Math.max(1, toInt(order.getOrDefault("total_item_count", 1))));
        addPointLedger(
                userId,
                StringUtils.hasText(bizTypeCode) ? bizTypeCode : "ORDER_REFUND",
                refund,
                reason + "｜" + goodsSummary
        );
        return true;
    }

    public synchronized boolean useAsset(Long userId, Long assetId) {
        Map<String, Object> asset = assets.get(assetId);
        if (asset == null) return false;
        if (!Objects.equals(userId, toLong(asset.get("user_id")))) return false;
        String status = String.valueOf(asset.get("asset_status_code"));
        int quantity = toInt(asset.get("quantity"));
        if (!(status.equals("ACTIVE") || status.equals("AVAILABLE")) || quantity <= 0) return false;

        asset.put("quantity", quantity - 1);
        if (quantity - 1 <= 0) {
            asset.put("asset_status_code", "USED");
        }

        Map<String, Object> flow = new HashMap<>();
        flow.put("id", assetFlowIdSeq.incrementAndGet());
        flow.put("asset_id", assetId);
        flow.put("flow_type_code", "USE");
        flow.put("delta_qty", -1);
        flow.put("occurred_at", now());
        flow.put("remark", "用户使用资产");
        assetFlows.computeIfAbsent(assetId, k -> new ArrayList<>()).add(flow);
        return true;
    }

    public List<Map<String, Object>> getPointLedgers(Long userId) {
        return pointLedgers.getOrDefault(userId, new ArrayList<>());
    }

    public void addPointLedger(Long userId, String bizTypeCode, Long changeAmount, String remark) {
        Map<String, Object> account = pointAccounts.get(userId);
        Map<String, Object> ledger = new HashMap<>();
        ledger.put("id", ledgerIdSeq.incrementAndGet());
        ledger.put("user_id", userId);
        ledger.put("biz_type_code", bizTypeCode);
        ledger.put("change_amount", changeAmount);
        ledger.put("balance_after", account.get("point_balance"));
        ledger.put("occurred_at", now());
        ledger.put("remark", remark);
        pointLedgers.computeIfAbsent(userId, k -> new ArrayList<>()).add(0, ledger);
    }

    private String buildOrderGoodsSummary(Map<String, Object> order, int quantity) {
        if (order == null) return "兑换商品";
        String productName = String.valueOf(order.getOrDefault("product_name_snapshot", ""));
        String skuName = String.valueOf(order.getOrDefault("sku_name_snapshot", ""));
        if (!StringUtils.hasText(productName)) {
            Long orderId = toLong(order.getOrDefault("id", 0));
            List<Map<String, Object>> items = orderItems.getOrDefault(orderId, new ArrayList<>());
            if (!items.isEmpty()) {
                Map<String, Object> first = items.get(0);
                productName = String.valueOf(first.getOrDefault("product_name_snapshot", first.getOrDefault("spu_name", "")));
                skuName = String.valueOf(first.getOrDefault("sku_name_snapshot", first.getOrDefault("sku_name", "")));
                quantity = Math.max(1, toInt(first.getOrDefault("quantity", quantity)));
            }
        }
        if (!StringUtils.hasText(productName)) productName = "兑换商品";
        if (!StringUtils.hasText(skuName)) skuName = "默认规格";
        return productName + " " + skuName + " x" + Math.max(1, quantity);
    }

    public List<Map<String, Object>> listOrdersByUser(Long userId, String orderStatusCode) {
        return orders.values().stream()
                .filter(item -> Objects.equals(toLong(item.get("user_id")), userId))
                .filter(item -> !StringUtils.hasText(orderStatusCode) || orderStatusCode.equals(item.get("order_status_code")))
                .sorted((a, b) -> String.valueOf(b.get("submit_at")).compareTo(String.valueOf(a.get("submit_at"))))
                .map(this::enrichOrderSnapshot)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getOrder(Long orderId) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) return null;
        return enrichOrderSnapshot(order);
    }

    public List<Map<String, Object>> getOrderItems(Long orderId) {
        return deduplicateOrderItems(orderItems.getOrDefault(orderId, new ArrayList<>())).stream()
                .map(this::enrichOrderItemSnapshot)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> deduplicateOrderItems(List<Map<String, Object>> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) return new ArrayList<>();
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> item : rawItems) {
            if (item == null || item.isEmpty()) continue;
            String key = buildOrderItemDedupKey(item);
            unique.putIfAbsent(key, item);
        }
        return new ArrayList<>(unique.values());
    }

    private String buildOrderItemDedupKey(Map<String, Object> item) {
        long spuId = toLong(item.getOrDefault("spu_id", item.getOrDefault("product_id", 0)));
        long skuId = toLong(item.getOrDefault("sku_id", 0));
        String productName = String.valueOf(item.getOrDefault(
                "product_name_snapshot",
                item.getOrDefault("spu_name", item.getOrDefault("product_name", ""))
        )).trim();
        String skuName = String.valueOf(item.getOrDefault("sku_name_snapshot", item.getOrDefault("sku_name", ""))).trim();
        long unitPoint = toLong(item.getOrDefault("unit_point_price", item.getOrDefault("point_price", 0)));
        int quantity = Math.max(1, toInt(item.getOrDefault("quantity", 1)));
        long totalPoint = toLong(item.getOrDefault("total_point_amount", unitPoint * quantity));
        return spuId + "|" + skuId + "|" + productName + "|" + skuName + "|" + unitPoint + "|" + quantity + "|" + totalPoint;
    }

    public List<Map<String, Object>> getOrderFlows(Long orderId) {
        return orderFlows.getOrDefault(orderId, new ArrayList<>());
    }

    public Map<String, Object> getOrderDelivery(Long orderId) {
        return orderDeliveries.get(orderId);
    }

    public Map<String, Object> getOrderAddressSnapshot(Long orderId) {
        return orderAddressSnapshots.get(orderId);
    }

    private Map<String, Object> enrichOrderSnapshot(Map<String, Object> order) {
        Map<String, Object> copy = new HashMap<>(order);
        Long orderId = toLong(copy.get("id"));
        String productName = String.valueOf(copy.getOrDefault("product_name_snapshot", ""));
        String mainImage = String.valueOf(copy.getOrDefault("main_image_snapshot", copy.getOrDefault("main_image_url", "")));
        List<Map<String, Object>> items = orderItems.getOrDefault(orderId, new ArrayList<>());
        if (!StringUtils.hasText(productName) && !items.isEmpty()) {
            productName = String.valueOf(items.get(0).getOrDefault("product_name_snapshot", ""));
        }
        Long spuId = items.isEmpty() ? 0L : toLong(items.get(0).getOrDefault("spu_id", 0));
        String latestImage = resolveProductMainImage(spuId, productName);
        if (StringUtils.hasText(latestImage)) {
            // 订单列表强制与首页商品主图保持一致
            mainImage = latestImage;
        } else if (!StringUtils.hasText(mainImage)) {
            for (Map<String, Object> item : items) {
                String fromItem = String.valueOf(item.getOrDefault("main_image_snapshot", item.getOrDefault("main_image_url", "")));
                if (StringUtils.hasText(fromItem)) {
                    mainImage = fromItem;
                    break;
                }
            }
        }
        copy.put("product_name_snapshot", productName);
        copy.put("main_image_snapshot", mainImage);
        return copy;
    }

    private Map<String, Object> enrichOrderItemSnapshot(Map<String, Object> item) {
        Map<String, Object> copy = new HashMap<>(item);
        String productName = String.valueOf(copy.getOrDefault("product_name_snapshot", ""));
        Long spuId = toLong(copy.getOrDefault("spu_id", 0));
        String mainImage = String.valueOf(copy.getOrDefault("main_image_snapshot", copy.getOrDefault("main_image_url", "")));
        String latestImage = resolveProductMainImage(spuId, productName);
        if (StringUtils.hasText(latestImage)) {
            // 订单详情同样跟随首页商品主图
            mainImage = latestImage;
        } else if (!StringUtils.hasText(mainImage)) {
            mainImage = resolveProductMainImage(spuId, productName);
        }
        copy.put("main_image_snapshot", mainImage);
        return copy;
    }

    private String resolveProductMainImage(Long spuId, String productName) {
        if (spuId != null && spuId > 0) {
            Map<String, Object> product = products.get(spuId);
            if (product != null) {
                String byId = String.valueOf(product.getOrDefault("main_image_url", ""));
                if (StringUtils.hasText(byId)) return byId;
            }
        }
        String orderName = compactProductName(productName);
        if (!StringUtils.hasText(orderName)) return "";
        for (Map<String, Object> product : products.values()) {
            String candidateName = compactProductName(String.valueOf(product.getOrDefault("product_name", "")));
            if (!StringUtils.hasText(candidateName)) continue;
            if (candidateName.contains(orderName) || orderName.contains(candidateName)) {
                String candidateImage = String.valueOf(product.getOrDefault("main_image_url", ""));
                if (StringUtils.hasText(candidateImage)) return candidateImage;
            }
        }
        return "";
    }

    private String compactProductName(String name) {
        if (!StringUtils.hasText(name)) return "";
        return name.replaceAll("[^\\p{IsHan}A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public List<Map<String, Object>> listAssetsByUser(Long userId) {
        return assets.values().stream()
                .filter(item -> Objects.equals(toLong(item.get("user_id")), userId))
                .sorted((a, b) -> Long.compare(toLong(b.get("id")), toLong(a.get("id"))))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getAsset(Long assetId) {
        return assets.get(assetId);
    }

    public List<Map<String, Object>> getAssetFlows(Long assetId) {
        return assetFlows.getOrDefault(assetId, new ArrayList<>());
    }

    public List<Map<String, Object>> listGroupResources() {
        return new ArrayList<>(groupResources.values());
    }

    public Map<String, Object> getGroupResource(Long id) {
        return groupResources.get(id);
    }

    public synchronized void upsertGroupResourceFromAdmin(Long id,
                                                          String groupName,
                                                          String appStatusCode,
                                                          String qrCodeUrl) {
        if (id == null) return;
        Map<String, Object> target = groupResources.computeIfAbsent(id, key -> new HashMap<>());
        target.put("id", id);
        if (StringUtils.hasText(groupName)) {
            target.put("resource_name", groupName);
        }
        if (StringUtils.hasText(appStatusCode)) {
            target.put("status_code", appStatusCode);
        }
        if (StringUtils.hasText(qrCodeUrl)) {
            target.put("qr_code_url", qrCodeUrl);
        }
        if (!target.containsKey("point_price")) {
            target.put("point_price", 0L);
        }
    }

    public synchronized void removeGroupResourceById(Long id) {
        if (id == null) return;
        groupResources.remove(id);
    }

    public Map<String, Object> getCustomerServiceContact() {
        return customerServiceContact;
    }

    public List<Map<String, Object>> getDictItems(String dictTypeCode) {
        return dictItems.getOrDefault(dictTypeCode, new ArrayList<>());
    }

    public List<Map<String, Object>> getPublicConfigs() {
        return publicConfigs;
    }

    public Map<String, Object> pageResult(List<Map<String, Object>> source, int pageNo, int pageSize) {
        int from = Math.max((pageNo - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, source.size());
        List<Map<String, Object>> list = from >= source.size() ? new ArrayList<>() : source.subList(from, to);
        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", pageNo);
        data.put("pageSize", pageSize);
        data.put("total", source.size());
        data.put("list", list);
        return data;
    }

    public int countOrdersByStatus(Long userId, String statusCode) {
        return (int) orders.values().stream()
                .filter(item -> Objects.equals(toLong(item.get("user_id")), userId))
                .filter(item -> statusCode.equals(item.get("order_status_code")))
                .count();
    }

    public int countOrders(Long userId) {
        return (int) orders.values().stream()
                .filter(item -> Objects.equals(toLong(item.get("user_id")), userId))
                .count();
    }

    public static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    public static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return 0;
        if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text) || "y".equalsIgnoreCase(text)) return 1;
        if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text) || "n".equalsIgnoreCase(text)) return 0;
        return Integer.parseInt(text);
    }

    private String mapCategoryStatusFromAdmin(String adminStatus) {
        if ("ACTIVE".equalsIgnoreCase(adminStatus) || "ENABLED".equalsIgnoreCase(adminStatus)) {
            return "ACTIVE";
        }
        return "DISABLED";
    }
}
