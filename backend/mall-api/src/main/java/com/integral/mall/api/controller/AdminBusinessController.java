package com.integral.mall.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integral.mall.api.common.ApiResponse;
import com.integral.mall.api.service.BizDataSyncService;
import com.integral.mall.api.service.JwtTokenService;
import com.integral.mall.api.service.KdniaoLogisticsService;
import com.integral.mall.api.service.RedisCacheService;
import com.integral.mall.api.service.UserBalanceReportService;
import com.integral.mall.api.store.InMemoryData;
import com.integral.mall.api.store.MySqlStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBusinessController {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessController.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String FILE_CACHE_CONTROL = "public, max-age=2592000, immutable";
    private static final String SNAPSHOT_KEY = "admin_business_state_v1";
    private static final int SKC_CODE_DIGITS = 12;
    private static final int SKU_CODE_DIGITS = 12;
    private static final int MAX_SPU_SKU_COUNT = 9;
    private static final int MAX_SPU_IMAGE_COUNT = 9;
    private static final Pattern ADMIN_FILE_CONTENT_URL_PATTERN =
            Pattern.compile("^https?://[^/]+(/api/v1/admin/files/\\d+/content)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WECHAT_DEFAULT_NICK_PATTERN =
            Pattern.compile("^微信用户\\d*$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_WISH_DEMAND_IMAGES = 9;

    private final AtomicLong pointLedgerIdSeq = new AtomicLong(50010);
    private final AtomicLong categoryIdSeq = new AtomicLong(20);
    private final AtomicLong slotIdSeq = new AtomicLong(220);
    private final AtomicLong recommendItemIdSeq = new AtomicLong(9100);
    private final AtomicLong spuIdSeq = new AtomicLong(110);
    private final AtomicLong skuIdSeq = new AtomicLong(5100);
    private final AtomicLong mediaIdSeq = new AtomicLong(8100);
    private final AtomicLong attrDefIdSeq = new AtomicLong(9100);
    private final AtomicLong attrValueIdSeq = new AtomicLong(9600);
    private final AtomicLong orderIdSeq = new AtomicLong(30010);
    private final AtomicLong orderFlowIdSeq = new AtomicLong(1100);
    private final AtomicLong assetIdSeq = new AtomicLong(7100);
    private final AtomicLong assetFlowIdSeq = new AtomicLong(91000);
    private final AtomicLong groupResourceIdSeq = new AtomicLong(8200);
    private final AtomicLong dictTypeIdSeq = new AtomicLong(1100);
    private final AtomicLong dictItemIdSeq = new AtomicLong(2200);
    private final AtomicLong configIdSeq = new AtomicLong(3100);
    private final AtomicLong fileIdSeq = new AtomicLong(4100);
    private final AtomicLong adminUserIdSeq = new AtomicLong(10);
    private final AtomicLong roleIdSeq = new AtomicLong(100);
    private final AtomicLong permissionIdSeq = new AtomicLong(600);
    private final AtomicLong pointRuleIdSeq = new AtomicLong(100);
    private final AtomicLong wishDemandIdSeq = new AtomicLong(10000);

    private final List<Map<String, Object>> users = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> userAddresses = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> pointLedger = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> categories = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> recommendSlots = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> recommendItems = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> spus = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> skus = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> medias = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> attrDefs = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> attrValues = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> orders = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> orderItems = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> orderFlows = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> orderDeliveries = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> backpackAssets = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> backpackFlows = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> groupResources = Collections.synchronizedList(new ArrayList<>());
    private final Set<Long> groupResourceDeletedIds = ConcurrentHashMap.newKeySet();

    private final List<Map<String, Object>> dictTypes = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> dictItems = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> systemConfigs = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> files = Collections.synchronizedList(new ArrayList<>());

    private final List<Map<String, Object>> adminUsers = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> roles = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> permissions = Collections.synchronizedList(new ArrayList<>());
    private final Map<Long, Set<Long>> rolePermissionMap = new ConcurrentHashMap<>();

    private final List<Map<String, Object>> pointRules = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> wishDemands = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private InMemoryData appStore;
    @Autowired(required = false)
    private MySqlStateStore mySqlStateStore;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired(required = false)
    private UserBalanceReportService userBalanceReportService;
    @Autowired(required = false)
    private KdniaoLogisticsService kdniaoLogisticsService;
    @Autowired(required = false)
    private BizDataSyncService bizDataSyncService;
    @Autowired(required = false)
    private RedisCacheService redisCacheService;
    @Autowired(required = false)
    private JwtTokenService jwtTokenService;
    @Value("${mall.file-storage-dir:./data/uploads}")
    private String localFileStorageDir;
    @Value("${mall.admin-state-file:./data/admin-business-state-v1.json}")
    private String localAdminStateFile;
    @Value("${mall.bootstrap-mock-data:false}")
    private boolean bootstrapMockData;
    @Value("${mall.require-mysql:false}")
    private boolean requireMysql;
    @Value("${mall.disable-local-snapshot:true}")
    private boolean disableLocalSnapshot;
    @Value("${mall.security.allow-legacy-token:false}")
    private boolean allowLegacyToken;
    private volatile Path localFileStorageRoot;

    public AdminBusinessController() {}

    @PostConstruct
    public void restoreStateFromMysql() {
        ensureLocalFileStorageRoot();
        boolean mysqlReady = mySqlStateStore != null && mySqlStateStore.isReady();
        if (requireMysql && !mysqlReady) {
            throw new IllegalStateException("MySQL 未就绪，已禁止使用内存/本地快照模式，请先检查数据库连接配置。");
        }
        boolean loadedFromLocalSnapshot = false;
        Optional<String> snapshot = Optional.empty();
        if (mysqlReady) {
            snapshot = mySqlStateStore.load(SNAPSHOT_KEY).filter(StringUtils::hasText);
        }
        if (snapshot.isEmpty() && !disableLocalSnapshot) {
            snapshot = loadSnapshotFromLocalFile().filter(StringUtils::hasText);
            loadedFromLocalSnapshot = snapshot.isPresent();
        }
        if (snapshot.isEmpty()) {
            if (bootstrapMockData) {
                initSeed();
                log.info("检测到空库快照，已初始化默认演示数据");
            } else {
                initMinimalSystemData();
                boolean loadedBiz = bootstrapCoreDataFromBusinessTables();
                if (loadedBiz) {
                    log.info("检测到空库快照，已从业务表初始化核心数据");
                } else {
                    log.info("检测到空库快照，已初始化最小系统数据（无业务模拟数据）");
                }
            }
            boolean changed = normalizeAllProductStructures();
            if (normalizeOrderProcurementStates()) changed = true;
            if (normalizeAdminUserPasswords()) changed = true;
            if (normalizeStoredFileAndMediaUrls()) changed = true;
            if (normalizeRecommendItemBannerUrls()) changed = true;
            if (migrateLegacyDataUrlFilesToLocalStorage()) changed = true;
            if (migrateLegacyDataUrlMediasToLocalStorage()) changed = true;
            persistStateToMysql();
            if (!mysqlReady && !requireMysql) {
                log.info("MySQL未就绪，已初始化并落本地快照");
            }
            syncMiniProgramDataFromAdminState();
            return;
        }
        try {
            Map<String, Object> state = objectMapper.readValue(snapshot.get(), new TypeReference<Map<String, Object>>() {});
            applySnapshot(state);
            syncIdSequencesByData();
            boolean changed = normalizeAllProductStructures();
            if (normalizeOrderProcurementStates()) changed = true;
            if (!bootstrapMockData && shouldBootstrapBusinessDataFromMysql()) {
                if (bootstrapCoreDataFromBusinessTables()) {
                    changed = true;
                    log.info("检测到快照内无商品数据，已从业务表自动修复");
                }
            }
            if (normalizeAdminUserPasswords()) changed = true;
            if (normalizeStoredFileAndMediaUrls()) changed = true;
            if (normalizeRecommendItemBannerUrls()) changed = true;
            if (migrateLegacyDataUrlFilesToLocalStorage()) changed = true;
            if (migrateLegacyDataUrlMediasToLocalStorage()) changed = true;
            if (changed || loadedFromLocalSnapshot) persistStateToMysql();
            if (loadedFromLocalSnapshot) {
                if (mysqlReady) {
                    log.info("管理端状态已从本地快照恢复，并回写MySQL");
                } else {
                    log.info("管理端状态已从本地快照恢复");
                }
            } else {
                log.info("管理端状态已从MySQL恢复");
            }
        } catch (Exception e) {
            log.warn("管理端状态恢复失败，将继续使用初始化数据: {}", e.getMessage());
            boolean changed = normalizeAllProductStructures();
            if (normalizeOrderProcurementStates()) changed = true;
            if (normalizeAdminUserPasswords()) changed = true;
            if (normalizeStoredFileAndMediaUrls()) changed = true;
            if (normalizeRecommendItemBannerUrls()) changed = true;
            if (migrateLegacyDataUrlFilesToLocalStorage()) changed = true;
            if (migrateLegacyDataUrlMediasToLocalStorage()) changed = true;
            persistStateToMysql();
        }
        syncMiniProgramDataFromAdminState();
    }

    public synchronized void persistStateToMysql() {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("users", cloneList(users));
            snapshot.put("userAddresses", cloneList(userAddresses));
            snapshot.put("pointLedger", cloneList(pointLedger));
            snapshot.put("categories", cloneList(categories));
            snapshot.put("recommendSlots", cloneList(recommendSlots));
            snapshot.put("recommendItems", cloneList(recommendItems));
            snapshot.put("spus", cloneList(spus));
            snapshot.put("skus", cloneList(skus));
            snapshot.put("medias", cloneList(medias));
            snapshot.put("attrDefs", cloneList(attrDefs));
            snapshot.put("attrValues", cloneList(attrValues));
            snapshot.put("orders", cloneList(orders));
            snapshot.put("orderItems", cloneList(orderItems));
            snapshot.put("orderFlows", cloneList(orderFlows));
            snapshot.put("orderDeliveries", cloneList(orderDeliveries));
            snapshot.put("backpackAssets", cloneList(backpackAssets));
            snapshot.put("backpackFlows", cloneList(backpackFlows));
            snapshot.put("groupResources", cloneList(groupResources));
            snapshot.put("dictTypes", cloneList(dictTypes));
            snapshot.put("dictItems", cloneList(dictItems));
            snapshot.put("systemConfigs", cloneList(systemConfigs));
            snapshot.put("files", cloneList(files));
            snapshot.put("adminUsers", cloneList(adminUsers));
            snapshot.put("roles", cloneList(roles));
            snapshot.put("permissions", cloneList(permissions));
            snapshot.put("pointRules", cloneList(pointRules));
            snapshot.put("wishDemands", cloneList(wishDemands));

            Map<String, List<Long>> roleMap = new LinkedHashMap<>();
            rolePermissionMap.forEach((k, v) -> roleMap.put(String.valueOf(k), new ArrayList<>(v)));
            snapshot.put("rolePermissionMap", roleMap);
            snapshot.put("groupResourceDeletedIds", new ArrayList<>(groupResourceDeletedIds));
            snapshot.put("sequences", exportSeqSnapshot());

            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            if (mySqlStateStore != null && mySqlStateStore.isReady()) {
                mySqlStateStore.save(SNAPSHOT_KEY, snapshotJson);
            }
            if (bizDataSyncService != null && bizDataSyncService.available()) {
                bizDataSyncService.syncCoreBusinessTables(
                        categories, spus, skus, medias, users,
                        userAddresses, pointLedger, orders, orderItems, orderFlows, orderDeliveries,
                        recommendSlots, recommendItems, systemConfigs
                );
            }
            if (redisCacheService != null) {
                redisCacheService.evictByPrefix("mall:app:home:");
                redisCacheService.evictByPrefix("mall:app:catalog:");
            }
            saveSnapshotToLocalFile(snapshotJson);
        } catch (Exception e) {
            log.warn("管理端状态持久化失败: {}", e.getMessage());
        }
    }

    public synchronized void syncAndPersistFromMiniProgram() {
        syncFromMiniProgramCoreData();
        persistStateToMysql();
    }

    // =========================
    // users & points
    // =========================
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String sortField,
                                                      @RequestParam(required = false) String sortOrder) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        String key = keyword == null ? "" : keyword.trim();
        String safeSortField = normalizeUserSortField(sortField);
        boolean sortAsc = isAscSortOrder(sortOrder);
        Comparator<Map<String, Object>> userComparator = buildUserSortComparator(safeSortField, sortAsc);
        List<Map<String, Object>> source = users.stream()
                .filter(item -> !StringUtils.hasText(key)
                        || matchKeyword(item, key, "nick_name", "phone_masked", "id", "admin_remark")
                        || matchUserByBackpackId(item, key)
                        || matchUserByOrderRemark(InMemoryData.toLong(item.get("id")), key))
                .peek(this::ensureUserExtraFields)
                .sorted(userComparator)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<Map<String, Object>> userDetail(HttpServletRequest request, @PathVariable Long userId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<String, Object> target = findById(users, userId);
        if (target == null) return ApiResponse.fail(4041, "用户不存在");
        ensureUserExtraFields(target);
        return ApiResponse.ok(copyMap(target));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> userStatus(HttpServletRequest request,
                                                       @PathVariable Long userId,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(users, userId);
        if (target == null) return ApiResponse.fail(4041, "用户不存在");
        String status = payload == null ? "" : String.valueOf(payload.getOrDefault("user_status_code", ""));
        if (!"ACTIVE".equals(status) && !"FROZEN".equals(status)) {
            return ApiResponse.fail(4001, "状态不合法");
        }
        target.put("user_status_code", status);
        syncSingleUserPointToMiniProgram(userId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/users/{userId}/points/adjust")
    public ApiResponse<Map<String, Object>> adjustUserPoint(HttpServletRequest request,
                                                            @PathVariable Long userId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(users, userId);
        if (target == null) return ApiResponse.fail(4041, "用户不存在");
        ensureUserExtraFields(target);

        long delta = InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("adjust_point", 0));
        double consumeAmount = toMoney(payload == null ? 0D : payload.getOrDefault("consume_amount", 0D));
        double drawUnitPrice = toMoney(payload == null ? 0D : payload.getOrDefault("draw_unit_price", 0D));
        double manualProfitAdjust = toMoney(payload == null ? 0D : payload.getOrDefault("manual_profit_adjust", 0D));
        String prizeName = String.valueOf(payload == null ? "" : payload.getOrDefault("prize_name", "")).trim();
        long drawCount = Math.max(0L, InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("draw_count", 0)));
        if (drawUnitPrice > 0D && drawCount > 0L) {
            consumeAmount = toMoney(drawUnitPrice * drawCount);
        }

        long before = InMemoryData.toLong(target.get("point_balance"));
        long after = Math.max(0, before + delta);
        target.put("point_balance", after);
        double totalConsumeBefore = toMoney(target.getOrDefault("total_consume_amount", 0D));
        double profitBefore = toMoney(target.getOrDefault("profit_amount", 0D));
        double tradeProfit = toMoney(delta - consumeAmount);
        double totalConsumeAfter = toMoney(totalConsumeBefore + consumeAmount);
        double profitAfter = toMoney(profitBefore + tradeProfit + manualProfitAdjust);
        target.put("total_consume_amount", totalConsumeAfter);
        target.put("profit_amount", profitAfter);

        Map<String, Object> ledger = new LinkedHashMap<>();
        ledger.put("id", pointLedgerIdSeq.incrementAndGet());
        ledger.put("user_id", userId);
        ledger.put("user_name", target.get("nick_name"));
        ledger.put("biz_type_code", "MANUAL_ADJUST");
        ledger.put("change_amount", delta);
        ledger.put("balance_after", after);
        String adjustRemark = String.valueOf(payload == null ? "" : payload.getOrDefault("adjust_remark", "")).trim();
        boolean adjustRemarkContainsHit = StringUtils.hasText(adjustRemark) && adjustRemark.contains("抽中");
        StringBuilder noteBuilder = new StringBuilder();
        if (StringUtils.hasText(prizeName) && !adjustRemarkContainsHit) {
            noteBuilder.append("抽中：").append(prizeName).append("，");
        }
        if (drawCount > 0) {
            noteBuilder.append("抽奖次数").append(drawCount).append("，");
        }
        if (consumeAmount != 0D) {
            noteBuilder.append("本次消费").append(moneyText(consumeAmount)).append("，");
        }
        if (tradeProfit != 0D) {
            noteBuilder.append("本次盈亏").append(moneyText(tradeProfit)).append("，");
        }
        if (manualProfitAdjust != 0D) {
            noteBuilder.append("手动盈亏调整").append(moneyText(manualProfitAdjust)).append("，");
        }
        if (StringUtils.hasText(adjustRemark)) {
            noteBuilder.append(adjustRemark);
        }
        String note = noteBuilder.toString().replaceAll("[，\\s]+$", "");
        ledger.put("note", note);
        ledger.put("consume_amount", consumeAmount);
        ledger.put("profit_change", toMoney(tradeProfit + manualProfitAdjust));
        ledger.put("prize_name", prizeName);
        ledger.put("draw_count", drawCount);
        ledger.put("occurred_at", now());
        pointLedger.add(0, ledger);
        syncSingleUserPointToMiniProgram(userId);
        persistStateToMysql();

        return ApiResponse.ok(Map.of(
                "success", true,
                "point_balance", after,
                "total_consume_amount", totalConsumeAfter,
                "profit_amount", profitAfter
        ));
    }

    @PostMapping("/users/{userId}/points/adjust/{ledgerId}/restore")
    public ApiResponse<Map<String, Object>> restoreUserPointAdjust(HttpServletRequest request,
                                                                   @PathVariable Long userId,
                                                                   @PathVariable Long ledgerId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(users, userId);
        if (target == null) return ApiResponse.fail(4041, "用户不存在");
        ensureUserExtraFields(target);

        Map<String, Object> sourceLedger = pointLedger.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("id")), ledgerId))
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("user_id")), userId))
                .findFirst()
                .orElse(null);
        if (sourceLedger == null) return ApiResponse.fail(4044, "调整记录不存在");

        String sourceBizType = String.valueOf(sourceLedger.getOrDefault("biz_type_code", ""));
        if (!"MANUAL_ADJUST".equalsIgnoreCase(sourceBizType)) {
            return ApiResponse.fail(4001, "仅支持恢复手动调整记录");
        }
        if (InMemoryData.toInt(sourceLedger.getOrDefault("restored_flag", 0)) == 1) {
            return ApiResponse.fail(4002, "该调整记录已恢复");
        }

        long sourceDelta = InMemoryData.toLong(sourceLedger.getOrDefault("change_amount", 0L));
        double sourceConsume = toMoney(sourceLedger.getOrDefault("consume_amount", 0D));
        double sourceProfitChange = toMoney(sourceLedger.getOrDefault("profit_change", 0D));

        long delta = -sourceDelta;
        double consumeAmount = toMoney(-sourceConsume);
        double profitChange = toMoney(-sourceProfitChange);
        double manualProfitAdjust = toMoney(profitChange - (delta - consumeAmount));

        long before = InMemoryData.toLong(target.getOrDefault("point_balance", 0L));
        long afterRaw = before + delta;
        if (afterRaw < 0L) {
            return ApiResponse.fail(4003, "当前碎片不足，无法恢复该记录");
        }
        long after = Math.max(0L, afterRaw);

        double totalConsumeBefore = toMoney(target.getOrDefault("total_consume_amount", 0D));
        double profitBefore = toMoney(target.getOrDefault("profit_amount", 0D));
        double totalConsumeAfter = toMoney(totalConsumeBefore + consumeAmount);
        double profitAfter = toMoney(profitBefore + profitChange);

        target.put("point_balance", after);
        target.put("total_consume_amount", totalConsumeAfter);
        target.put("profit_amount", profitAfter);

        long restoreLedgerId = pointLedgerIdSeq.incrementAndGet();
        Map<String, Object> ledger = new LinkedHashMap<>();
        ledger.put("id", restoreLedgerId);
        ledger.put("user_id", userId);
        ledger.put("user_name", target.get("nick_name"));
        ledger.put("biz_type_code", "MANUAL_ADJUST_RESTORE");
        ledger.put("change_amount", delta);
        ledger.put("balance_after", after);
        ledger.put("consume_amount", consumeAmount);
        ledger.put("profit_change", profitChange);
        ledger.put("manual_profit_adjust", manualProfitAdjust);
        ledger.put("restore_of_ledger_id", ledgerId);
        ledger.put("occurred_at", now());

        String sourceNote = String.valueOf(sourceLedger.getOrDefault("note", "")).trim();
        String note = String.format(Locale.ROOT,
                "恢复调整#%d，原变动%s碎片，原消费%s，原盈亏%s%s",
                ledgerId,
                sourceDelta >= 0 ? "+" + sourceDelta : String.valueOf(sourceDelta),
                moneyText(sourceConsume),
                moneyText(sourceProfitChange),
                StringUtils.hasText(sourceNote) ? "，原备注：" + sourceNote : "");
        ledger.put("note", note);

        pointLedger.add(0, ledger);

        sourceLedger.put("restored_flag", 1);
        sourceLedger.put("restored_at", now());
        sourceLedger.put("restore_by_ledger_id", restoreLedgerId);

        syncSingleUserPointToMiniProgram(userId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of(
                "success", true,
                "point_balance", after,
                "total_consume_amount", totalConsumeAfter,
                "profit_amount", profitAfter
        ));
    }

    @PutMapping("/users/{userId}/remark")
    public ApiResponse<Map<String, Object>> updateUserRemark(HttpServletRequest request,
                                                             @PathVariable Long userId,
                                                             @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(users, userId);
        if (target == null) return ApiResponse.fail(4041, "用户不存在");
        target.put("admin_remark", String.valueOf(payload == null ? "" : payload.getOrDefault("admin_remark", "")));
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/users/{userId}/finance")
    public ApiResponse<Map<String, Object>> updateUserFinance(HttpServletRequest request,
                                                              @PathVariable Long userId,
                                                              @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(users, userId);
        if (target == null) return ApiResponse.fail(4041, "用户不存在");
        ensureUserExtraFields(target);

        double totalConsumeAmount = payload != null && payload.containsKey("total_consume_amount")
                ? toMoney(payload.get("total_consume_amount"))
                : toMoney(target.getOrDefault("total_consume_amount", 0D));
        double profitAmount = payload != null && payload.containsKey("profit_amount")
                ? toMoney(payload.get("profit_amount"))
                : toMoney(target.getOrDefault("profit_amount", 0D));

        target.put("total_consume_amount", totalConsumeAmount);
        target.put("profit_amount", profitAmount);
        syncSingleUserPointToMiniProgram(userId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of(
                "success", true,
                "total_consume_amount", totalConsumeAmount,
                "profit_amount", profitAmount
        ));
    }

    @GetMapping("/users/{userId}/points/ledger")
    public ApiResponse<Map<String, Object>> userPointLedger(HttpServletRequest request,
                                                            @PathVariable Long userId,
                                                            @RequestParam(defaultValue = "1") int pageNo,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = pointLedger.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("user_id")), userId))
                .sorted((a, b) -> {
                    int cmp = String.valueOf(b.getOrDefault("occurred_at", "")).compareTo(String.valueOf(a.getOrDefault("occurred_at", "")));
                    if (cmp != 0) return cmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/users/{userId}/orders")
    public ApiResponse<Map<String, Object>> userOrders(HttpServletRequest request,
                                                       @PathVariable Long userId,
                                                       @RequestParam(defaultValue = "1") int pageNo,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        requireAdminLike(request);
        List<Map<String, Object>> source = orders.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("user_id")), userId))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/users/{userId}/addresses")
    public ApiResponse<Map<String, Object>> userAddresses(HttpServletRequest request,
                                                          @PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "20") int pageSize) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = userAddresses.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("user_id")), userId))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/points/ledger")
    public ApiResponse<Map<String, Object>> allPointLedger(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = pointLedger.stream()
                .sorted((a, b) -> {
                    int cmp = String.valueOf(b.getOrDefault("occurred_at", "")).compareTo(String.valueOf(a.getOrDefault("occurred_at", "")));
                    if (cmp != 0) return cmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/points/rules")
    public ApiResponse<Map<String, Object>> listPointRules(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        requireAdminLike(request);
        return ApiResponse.ok(pageResult(new ArrayList<>(pointRules), pageNo, pageSize));
    }

    @PostMapping("/points/rules")
    public ApiResponse<Map<String, Object>> createPointRule(HttpServletRequest request,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", pointRuleIdSeq.incrementAndGet());
        item.put("rule_name", String.valueOf(payload == null ? "新规则" : payload.getOrDefault("rule_name", "新规则")));
        item.put("rule_code", String.valueOf(payload == null ? "RULE_NEW" : payload.getOrDefault("rule_code", "RULE_NEW")));
        item.put("change_value", InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("change_value", 0)));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("updated_at", now());
        pointRules.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/points/rules/{ruleId}")
    public ApiResponse<Map<String, Object>> updatePointRule(HttpServletRequest request,
                                                            @PathVariable Long ruleId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(pointRules, ruleId);
        if (target == null) return ApiResponse.fail(4041, "规则不存在");
        mergeFields(target, payload, "rule_name", "rule_code", "change_value", "status_code", "remark");
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/points/rules/{ruleId}/status")
    public ApiResponse<Map<String, Object>> updatePointRuleStatus(HttpServletRequest request,
                                                                  @PathVariable Long ruleId,
                                                                  @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(pointRules, ruleId);
        if (target == null) return ApiResponse.fail(4041, "规则不存在");
        target.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // categories
    // =========================
    @GetMapping("/categories")
    public ApiResponse<Map<String, Object>> listCategories(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize,
                                                           @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = categories.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "category_name", "id"))
                .sorted((a, b) -> InMemoryData.toInt(b.get("sort_no")) - InMemoryData.toInt(a.get("sort_no")))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/categories")
    public ApiResponse<Map<String, Object>> createCategory(HttpServletRequest request,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", categoryIdSeq.incrementAndGet());
        item.put("category_name", String.valueOf(payload == null ? "新分类" : payload.getOrDefault("category_name", "新分类")));
        item.put("sort_no", InMemoryData.toInt(payload == null ? 100 : payload.getOrDefault("sort_no", 100)));
        item.put("status_code", "ENABLED");
        item.put("product_count", 0);
        item.put("updated_at", now());
        categories.add(item);
        syncCategoryToMiniProgram(item);
        return ApiResponse.ok(copyMap(item));
    }

    @GetMapping("/categories/{categoryId}")
    public ApiResponse<Map<String, Object>> categoryDetail(HttpServletRequest request, @PathVariable Long categoryId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<String, Object> target = findById(categories, categoryId);
        if (target == null) return ApiResponse.fail(4041, "分类不存在");
        return ApiResponse.ok(copyMap(target));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<Map<String, Object>> updateCategory(HttpServletRequest request,
                                                           @PathVariable Long categoryId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(categories, categoryId);
        if (target == null) return ApiResponse.fail(4041, "分类不存在");
        mergeFields(target, payload, "category_name", "sort_no");
        target.put("updated_at", now());
        syncCategoryToMiniProgram(target);
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Map<String, Object>> deleteCategory(HttpServletRequest request, @PathVariable Long categoryId) {
        requireAdminLike(request);
        removeById(categories, categoryId);
        if (appStore != null) {
            appStore.removeCategoryById(categoryId);
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/categories/{categoryId}/status")
    public ApiResponse<Map<String, Object>> categoryStatus(HttpServletRequest request,
                                                           @PathVariable Long categoryId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(categories, categoryId);
        if (target == null) return ApiResponse.fail(4041, "分类不存在");
        target.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        target.put("updated_at", now());
        syncCategoryToMiniProgram(target);
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/categories/sort")
    public ApiResponse<Map<String, Object>> categorySort(HttpServletRequest request,
                                                         @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Object arr = payload == null ? null : payload.get("categories");
        if (arr instanceof Collection<?>) {
            for (Object one : (Collection<?>) arr) {
                if (!(one instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) one;
                Long categoryId = InMemoryData.toLong(map.get("category_id"));
                int sortNo = InMemoryData.toInt(map.get("sort_no"));
                Map<String, Object> target = findById(categories, categoryId);
                if (target != null) {
                    target.put("sort_no", sortNo);
                    target.put("updated_at", now());
                    syncCategoryToMiniProgram(target);
                }
            }
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // recommend slots & items
    // =========================
    @GetMapping("/recommend-slots")
    public ApiResponse<Map<String, Object>> listRecommendSlots(HttpServletRequest request,
                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                               @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = recommendSlots.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "slot_name", "slot_code"))
                .map(item -> {
                    Map<String, Object> copy = copyMap(item);
                    long slotId = InMemoryData.toLong(copy.get("id"));
                    long count = recommendItems.stream().filter(one -> Objects.equals(InMemoryData.toLong(one.get("slot_id")), slotId)).count();
                    copy.put("item_count", count);
                    return copy;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/recommend-slots")
    public ApiResponse<Map<String, Object>> createRecommendSlot(HttpServletRequest request,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", slotIdSeq.incrementAndGet());
        item.put("slot_name", String.valueOf(payload == null ? "新推荐位" : payload.getOrDefault("slot_name", "新推荐位")));
        item.put("slot_code", String.valueOf(payload == null ? "NEW_SLOT" : payload.getOrDefault("slot_code", "NEW_SLOT")));
        item.put("status_code", "ENABLED");
        item.put("updated_at", now());
        recommendSlots.add(0, item);
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/recommend-slots/{slotId}")
    public ApiResponse<Map<String, Object>> updateRecommendSlot(HttpServletRequest request,
                                                                @PathVariable Long slotId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(recommendSlots, slotId);
        if (target == null) return ApiResponse.fail(4041, "推荐位不存在");
        mergeFields(target, payload, "slot_name", "slot_code");
        target.put("updated_at", now());
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/recommend-slots/{slotId}/status")
    public ApiResponse<Map<String, Object>> recommendSlotStatus(HttpServletRequest request,
                                                                @PathVariable Long slotId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(recommendSlots, slotId);
        if (target == null) return ApiResponse.fail(4041, "推荐位不存在");
        target.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        target.put("updated_at", now());
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/recommend-slots/{slotId}/items")
    public ApiResponse<Map<String, Object>> listRecommendItems(HttpServletRequest request, @PathVariable Long slotId) {
        requireAdminLike(request);
        List<Map<String, Object>> source = recommendItems.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("slot_id")), slotId))
                .sorted((a, b) -> InMemoryData.toInt(b.get("sort_no")) - InMemoryData.toInt(a.get("sort_no")))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", source));
    }

    @PostMapping("/recommend-slots/{slotId}/items")
    public ApiResponse<Map<String, Object>> createRecommendItem(HttpServletRequest request,
                                                                @PathVariable Long slotId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", recommendItemIdSeq.incrementAndGet());
        item.put("slot_id", slotId);
        item.put("spu_id", InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("spu_id", 0)));
        item.put("product_name", String.valueOf(payload == null ? "新推荐商品" : payload.getOrDefault("product_name", "新推荐商品")));
        item.put("point_price", InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("point_price", 0)));
        item.put("sort_no", InMemoryData.toInt(payload == null ? 100 : payload.getOrDefault("sort_no", 100)));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("start_at", String.valueOf(payload == null ? now() : payload.getOrDefault("start_at", now())));
        item.put("end_at", String.valueOf(payload == null ? "2099-12-31 23:59:59" : payload.getOrDefault("end_at", "2099-12-31 23:59:59")));
        String bannerImageUrl = resolveRecommendItemBannerUrl(payload);
        if (StringUtils.hasText(bannerImageUrl)) {
            item.put("banner_image_url", bannerImageUrl);
            item.put("image_url", bannerImageUrl);
        }
        recommendItems.add(0, item);
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/recommend-items/{itemId}")
    public ApiResponse<Map<String, Object>> updateRecommendItem(HttpServletRequest request,
                                                                @PathVariable Long itemId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(recommendItems, itemId);
        if (target == null) return ApiResponse.fail(4041, "推荐项不存在");
        mergeFields(target, payload, "spu_id", "product_name", "point_price", "sort_no", "status_code", "start_at", "end_at");
        if (containsRecommendItemBannerUrlKey(payload)) {
            String bannerImageUrl = resolveRecommendItemBannerUrl(payload);
            if (StringUtils.hasText(bannerImageUrl)) {
                target.put("banner_image_url", bannerImageUrl);
                target.put("image_url", bannerImageUrl);
            } else {
                target.remove("banner_image_url");
                target.remove("image_url");
            }
        }
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/recommend-items/{itemId}")
    public ApiResponse<Map<String, Object>> deleteRecommendItem(HttpServletRequest request, @PathVariable Long itemId) {
        requireAdminLike(request);
        removeById(recommendItems, itemId);
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/recommend-slots/{slotId}/items/sort")
    public ApiResponse<Map<String, Object>> sortRecommendItems(HttpServletRequest request,
                                                               @PathVariable Long slotId,
                                                               @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Object arr = payload == null ? null : payload.get("items");
        if (arr instanceof Collection<?>) {
            for (Object one : (Collection<?>) arr) {
                if (!(one instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) one;
                Long itemId = InMemoryData.toLong(map.get("item_id"));
                Map<String, Object> target = findById(recommendItems, itemId);
                if (target != null && Objects.equals(InMemoryData.toLong(target.get("slot_id")), slotId)) {
                    target.put("sort_no", InMemoryData.toInt(map.get("sort_no")));
                }
            }
        }
        syncHomeBannersFromRecommendSlots();
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // products
    // =========================
    @GetMapping("/products/spu")
    public ApiResponse<Map<String, Object>> listSpu(HttpServletRequest request,
                                                    @RequestParam(defaultValue = "1") int pageNo,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false, name = "status_code") String statusCode) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        for (Map<String, Object> spu : spus) {
            ensureSpuCodes(spu);
        }
        List<Map<String, Object>> source = spus.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "spu_name", "category_name", "id"))
                .filter(item -> !StringUtils.hasText(statusCode) || Objects.equals(statusCode, item.get("status_code")))
                .sorted((a, b) -> String.valueOf(b.get("updated_at")).compareTo(String.valueOf(a.get("updated_at"))))
                .map(item -> {
                    Map<String, Object> mapped = copyMap(item);
                    Long spuId = InMemoryData.toLong(item.get("id"));
                    long skuCount = skus.stream().filter(sku -> Objects.equals(InMemoryData.toLong(sku.get("spu_id")), spuId)).count();
                    mapped.put("sku_count", skuCount);
                    return mapped;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/products/spu")
    public ApiResponse<Map<String, Object>> createSpu(HttpServletRequest request,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        long id = spuIdSeq.incrementAndGet();

        List<Map<String, Object>> skuInputs = new ArrayList<>();
        Object rawSkuList = payload == null ? null : payload.get("sku_list");
        if (rawSkuList instanceof List) {
            if (((List<?>) rawSkuList).size() > MAX_SPU_SKU_COUNT) {
                return ApiResponse.fail(4001, "单个商品最多支持9个SKU");
            }
            for (Object raw : (List<?>) rawSkuList) {
                if (!(raw instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> input = (Map<String, Object>) raw;
                String skuName = String.valueOf(input.getOrDefault("sku_name", "")).trim();
                if (!StringUtils.hasText(skuName)) continue;
                Map<String, Object> normalized = new LinkedHashMap<>();
                normalized.put("sku_name", skuName);
                normalized.put("spec_text", String.valueOf(input.getOrDefault("spec_text", "默认")).trim());
                normalized.put("point_price", Math.max(1L, InMemoryData.toLong(input.getOrDefault("point_price", 1))));
                normalized.put("stock_available", Math.max(0L, InMemoryData.toLong(input.getOrDefault("stock_available", 0))));
                normalized.put("status_code", String.valueOf(input.getOrDefault("status_code", "ENABLED")));
                skuInputs.add(normalized);
                if (skuInputs.size() >= MAX_SPU_SKU_COUNT) break;
            }
        }
        if (skuInputs.isEmpty()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("sku_name", String.valueOf(payload == null ? "默认规格" : payload.getOrDefault("default_sku_name", "默认规格")));
            fallback.put("spec_text", String.valueOf(payload == null ? "默认" : payload.getOrDefault("default_spec_text", "默认")));
            fallback.put("point_price", Math.max(1L, InMemoryData.toLong(payload == null ? 1 : payload.getOrDefault("point_price_min", 1))));
            fallback.put("stock_available", Math.max(0L, InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("stock_available", 0))));
            fallback.put("status_code", "ENABLED");
            skuInputs.add(fallback);
        }

        long pointPriceMin = skuInputs.stream().mapToLong(item -> InMemoryData.toLong(item.getOrDefault("point_price", 1))).min().orElse(1L);
        long pointPriceMax = skuInputs.stream().mapToLong(item -> InMemoryData.toLong(item.getOrDefault("point_price", 1))).max().orElse(pointPriceMin);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("skc_code", buildSkcCode(id));
        item.put("spu_name", String.valueOf(payload == null ? "新商品" : payload.getOrDefault("spu_name", "新商品")));
        List<String> categoryNames = resolveCategoryNames(payload, "未分类");
        item.put("category_name", String.join(" / ", categoryNames));
        item.put("category_names", new ArrayList<>(categoryNames));
        List<Long> categoryIds = resolveCategoryIdsByNames(categoryNames);
        if (!categoryIds.isEmpty()) {
            item.put("category_id", categoryIds.get(0));
            item.put("category_ids", new ArrayList<>(categoryIds));
        }
        item.put("point_price_min", pointPriceMin);
        item.put("point_price_max", pointPriceMax);
        item.put("total_stock", skuInputs.stream().mapToLong(sku -> Math.max(0L, InMemoryData.toLong(sku.getOrDefault("stock_available", 0)))).sum());
        item.put("detail_html", payload == null || payload.get("detail_html") == null ? "" : String.valueOf(payload.get("detail_html")));
        item.put("status_code", "OFF_SHELF");
        item.put("recommend_flag", false);
        item.put("updated_at", now());
        spus.add(0, item);

        // 创建时允许一次录入 1~9 个 SKU
        for (Map<String, Object> skuInput : skuInputs) {
            Map<String, Object> sku = new LinkedHashMap<>();
            long skuId = skuIdSeq.incrementAndGet();
            sku.put("id", skuId);
            sku.put("sku_code", buildSkuCode(skuId));
            sku.put("spu_id", id);
            sku.put("sku_name", String.valueOf(skuInput.getOrDefault("sku_name", "默认规格")));
            sku.put("spec_text", String.valueOf(skuInput.getOrDefault("spec_text", "默认")));
            sku.put("point_price", InMemoryData.toLong(skuInput.getOrDefault("point_price", 1)));
            sku.put("stock_available", InMemoryData.toLong(skuInput.getOrDefault("stock_available", 0)));
            sku.put("status_code", String.valueOf(skuInput.getOrDefault("status_code", "ENABLED")));
            skus.add(0, sku);
        }

        List<String> imageUrls = resolveSpuImageUrlsFromPayload(payload);
        if (!imageUrls.isEmpty()) {
            createSpuImageMedias(id, imageUrls);
        }
        syncSpu(id);
        syncAllSpusToMiniProgram();
        syncAllProductMainImagesToMiniProgram();
        persistStateToMysql();
        return ApiResponse.ok(copyMap(item));
    }

    @GetMapping("/products/spu/{spuId}")
    public ApiResponse<Map<String, Object>> spuDetail(HttpServletRequest request, @PathVariable Long spuId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        boolean legacyMediaMigrated = migrateLegacyDataUrlMedias(spuId);
        boolean mediaChanged = enforceMaxImageMediaForSpu(spuId);
        if (legacyMediaMigrated || mediaChanged) persistStateToMysql();
        Map<String, Object> target = findById(spus, spuId);
        if (target == null) return ApiResponse.fail(4041, "SPU不存在");
        Map<String, Object> data = copyMap(target);
        List<Map<String, Object>> mediaList = medias.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .sorted((a, b) -> InMemoryData.toInt(b.get("sort_no")) - InMemoryData.toInt(a.get("sort_no")))
                .map(this::copyMap)
                .collect(Collectors.toList());
        data.put("media_list", mediaList);
        return ApiResponse.ok(data);
    }

    @PutMapping("/products/spu/{spuId}")
    public ApiResponse<Map<String, Object>> updateSpu(HttpServletRequest request,
                                                      @PathVariable Long spuId,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(spus, spuId);
        if (target == null) return ApiResponse.fail(4041, "SPU不存在");
        mergeFields(target, payload, "spu_name", "point_price_min", "point_price_max", "detail_html");
        List<String> categoryNames = resolveCategoryNames(payload, String.valueOf(target.getOrDefault("category_name", "未分类")));
        target.put("category_name", String.join(" / ", categoryNames));
        target.put("category_names", new ArrayList<>(categoryNames));
        List<Long> categoryIds = resolveCategoryIdsByNames(categoryNames);
        if (!categoryIds.isEmpty()) {
            target.put("category_id", categoryIds.get(0));
            target.put("category_ids", new ArrayList<>(categoryIds));
            if (appStore != null) {
                appStore.updateProductCategories(spuId, categoryIds);
            }
        }
        target.put("updated_at", now());
        syncSpu(spuId);
        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/products/spu/{spuId}")
    public ApiResponse<Map<String, Object>> deleteSpu(HttpServletRequest request, @PathVariable Long spuId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(spus, spuId);
        if (target == null) return ApiResponse.fail(4041, "SPU不存在");

        removeById(spus, spuId);
        skus.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId));
        medias.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId));
        attrValues.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId));
        recommendItems.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId));

        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        syncHomeBannersFromRecommendSlots();
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/products/spu/{spuId}/status")
    public ApiResponse<Map<String, Object>> spuStatus(HttpServletRequest request,
                                                      @PathVariable Long spuId,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(spus, spuId);
        if (target == null) return ApiResponse.fail(4041, "SPU不存在");
        String nextStatus = String.valueOf(payload == null ? "OFF_SHELF" : payload.getOrDefault("status_code", "OFF_SHELF"));
        target.put("status_code", nextStatus);
        List<Map<String, Object>> spuSkus = skus.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .collect(Collectors.toList());
        if (spuSkus.isEmpty()) {
            Map<String, Object> sku = new LinkedHashMap<>();
            long skuId = skuIdSeq.incrementAndGet();
            sku.put("id", skuId);
            sku.put("sku_code", buildSkuCode(skuId));
            sku.put("spu_id", spuId);
            sku.put("sku_name", "默认规格");
            sku.put("spec_text", "默认");
            sku.put("point_price", InMemoryData.toLong(target.getOrDefault("point_price_min", 1)));
            sku.put("stock_available", InMemoryData.toLong(target.getOrDefault("total_stock", 0)));
            sku.put("status_code", "ON_SHELF".equalsIgnoreCase(nextStatus) ? "ENABLED" : "DISABLED");
            skus.add(0, sku);
        } else {
            spuSkus.forEach(item -> item.put("status_code", "ON_SHELF".equalsIgnoreCase(nextStatus) ? "ENABLED" : "DISABLED"));
        }
        target.put("updated_at", now());
        syncSpu(spuId);
        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/products/spu/{spuId}/recommend")
    public ApiResponse<Map<String, Object>> spuRecommend(HttpServletRequest request,
                                                         @PathVariable Long spuId,
                                                         @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(spus, spuId);
        if (target == null) return ApiResponse.fail(4041, "SPU不存在");
        Object value = payload == null ? Boolean.FALSE : payload.getOrDefault("recommend_flag", Boolean.FALSE);
        target.put("recommend_flag", Boolean.TRUE.equals(value));
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/products/spu/{spuId}/skus")
    public ApiResponse<Map<String, Object>> listSkus(HttpServletRequest request, @PathVariable Long spuId) {
        requireAdminLike(request);
        boolean changed = enforceMaxSkuCountForSpu(spuId);
        if (changed) persistStateToMysql();
        List<Map<String, Object>> source = skus.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", source));
    }

    @PostMapping("/products/spu/{spuId}/skus")
    public ApiResponse<Map<String, Object>> createSku(HttpServletRequest request,
                                                      @PathVariable Long spuId,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> spu = findById(spus, spuId);
        if (spu == null) return ApiResponse.fail(4041, "SPU不存在");
        long exists = skus.stream().filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId)).count();
        if (exists >= MAX_SPU_SKU_COUNT) {
            return ApiResponse.fail(4001, "当前商品最多支持9个SKU，请编辑已有SKU");
        }

        Map<String, Object> sku = new LinkedHashMap<>();
        long skuId = skuIdSeq.incrementAndGet();
        sku.put("id", skuId);
        sku.put("sku_code", buildSkuCode(skuId));
        sku.put("spu_id", spuId);
        sku.put("sku_name", String.valueOf(payload == null ? "默认规格" : payload.getOrDefault("sku_name", "默认规格")));
        sku.put("spec_text", String.valueOf(payload == null ? "默认" : payload.getOrDefault("spec_text", "默认")));
        sku.put("point_price", InMemoryData.toLong(payload == null ? 1 : payload.getOrDefault("point_price", 1)));
        sku.put("stock_available", InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("stock_available", 0)));
        sku.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        skus.add(0, sku);
        syncSpu(spuId);
        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        persistStateToMysql();
        return ApiResponse.ok(copyMap(sku));
    }

    @PutMapping("/products/skus/{skuId}")
    public ApiResponse<Map<String, Object>> updateSku(HttpServletRequest request,
                                                      @PathVariable Long skuId,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(skus, skuId);
        if (target == null) return ApiResponse.fail(4041, "SKU不存在");
        ensureSkuCodes(target);
        mergeFields(target, payload, "sku_name", "spec_text", "point_price", "stock_available", "status_code");
        syncSpu(InMemoryData.toLong(target.get("spu_id")));
        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/products/skus/{skuId}/stock")
    public ApiResponse<Map<String, Object>> updateSkuStock(HttpServletRequest request,
                                                           @PathVariable Long skuId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(skus, skuId);
        if (target == null) return ApiResponse.fail(4041, "SKU不存在");
        long delta = InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("delta_stock", 0));
        long current = InMemoryData.toLong(target.get("stock_available"));
        target.put("stock_available", Math.max(0, current + delta));
        syncSpu(InMemoryData.toLong(target.get("spu_id")));
        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/products/spu/{spuId}/media")
    public ApiResponse<Map<String, Object>> createSpuMedia(HttpServletRequest request,
                                                           @PathVariable Long spuId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> spu = findById(spus, spuId);
        if (spu == null) return ApiResponse.fail(4041, "SPU不存在");
        String mediaType = String.valueOf(payload == null ? "IMAGE" : payload.getOrDefault("media_type", "IMAGE"));
        String rawMediaUrl = String.valueOf(payload == null ? "" : payload.getOrDefault("media_url", ""));
        String mediaUrl = normalizeMediaUrl(rawMediaUrl);
        int sortNo = InMemoryData.toInt(payload == null ? 100 : payload.getOrDefault("sort_no", 100));

        if ("IMAGE".equalsIgnoreCase(mediaType)) {
            long imageCount = medias.stream()
                    .filter(existing -> Objects.equals(InMemoryData.toLong(existing.get("spu_id")), spuId))
                    .filter(existing -> "IMAGE".equalsIgnoreCase(String.valueOf(existing.getOrDefault("media_type", "IMAGE"))))
                    .count();
            if (imageCount >= MAX_SPU_IMAGE_COUNT) {
                return ApiResponse.fail(4001, "商品图片最多支持9张");
            }
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", mediaIdSeq.incrementAndGet());
        item.put("spu_id", spuId);
        medias.add(0, item);
        item.put("media_type", mediaType);
        item.put("media_url", mediaUrl);
        item.put("sort_no", sortNo);
        if ("IMAGE".equalsIgnoreCase(mediaType)) {
            enforceMaxImageMediaForSpu(spuId);
        }
        syncSpuMainImageToMiniProgram(spuId);
        syncHomeBannersFromRecommendSlots();
        persistStateToMysql();
        return ApiResponse.ok(copyMap(item));
    }

    @DeleteMapping("/products/media/{mediaId}")
    public ApiResponse<Map<String, Object>> deleteSpuMedia(HttpServletRequest request, @PathVariable Long mediaId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(medias, mediaId);
        Long spuId = target == null ? null : InMemoryData.toLong(target.get("spu_id"));
        removeById(medias, mediaId);
        if (spuId != null) {
            syncSpuMainImageToMiniProgram(spuId);
            syncHomeBannersFromRecommendSlots();
        }
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/products/attr-defs")
    public ApiResponse<Map<String, Object>> listAttrDefs(HttpServletRequest request,
                                                         @RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = attrDefs.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "attr_name", "attr_code"))
                .sorted((a, b) -> String.valueOf(b.get("updated_at")).compareTo(String.valueOf(a.get("updated_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/products/attr-defs")
    public ApiResponse<Map<String, Object>> createAttrDef(HttpServletRequest request,
                                                          @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", attrDefIdSeq.incrementAndGet());
        item.put("attr_name", String.valueOf(payload == null ? "新属性" : payload.getOrDefault("attr_name", "新属性")));
        item.put("attr_code", String.valueOf(payload == null ? "NEW_ATTR" : payload.getOrDefault("attr_code", "NEW_ATTR")));
        item.put("value_type", String.valueOf(payload == null ? "TEXT" : payload.getOrDefault("value_type", "TEXT")));
        item.put("required_flag", Boolean.TRUE.equals(payload == null ? Boolean.FALSE : payload.getOrDefault("required_flag", Boolean.FALSE)));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("updated_at", now());
        attrDefs.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/products/attr-defs/{attrDefId}")
    public ApiResponse<Map<String, Object>> updateAttrDef(HttpServletRequest request,
                                                          @PathVariable Long attrDefId,
                                                          @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(attrDefs, attrDefId);
        if (target == null) return ApiResponse.fail(4041, "属性不存在");
        mergeFields(target, payload, "attr_name", "attr_code", "value_type", "required_flag", "status_code");
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/products/attr-defs/{attrDefId}")
    public ApiResponse<Map<String, Object>> deleteAttrDef(HttpServletRequest request, @PathVariable Long attrDefId) {
        requireAdminLike(request);
        removeById(attrDefs, attrDefId);
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/products/{spuId}/attr-values")
    public ApiResponse<Map<String, Object>> listAttrValues(HttpServletRequest request,
                                                           @PathVariable Long spuId,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "50") int pageSize) {
        requireAdminLike(request);
        List<Map<String, Object>> source = attrValues.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/products/{spuId}/attr-values")
    public ApiResponse<Map<String, Object>> createAttrValue(HttpServletRequest request,
                                                            @PathVariable Long spuId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", attrValueIdSeq.incrementAndGet());
        item.put("spu_id", spuId);
        item.put("attr_def_id", InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("attr_def_id", 0)));
        item.put("attr_value", String.valueOf(payload == null ? "" : payload.getOrDefault("attr_value", "")));
        item.put("updated_at", now());
        attrValues.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/products/attr-values/{attrValueId}")
    public ApiResponse<Map<String, Object>> updateAttrValue(HttpServletRequest request,
                                                            @PathVariable Long attrValueId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(attrValues, attrValueId);
        if (target == null) return ApiResponse.fail(4041, "属性值不存在");
        mergeFields(target, payload, "attr_value", "attr_def_id");
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/products/attr-values/{attrValueId}")
    public ApiResponse<Map<String, Object>> deleteAttrValue(HttpServletRequest request, @PathVariable Long attrValueId) {
        requireAdminLike(request);
        removeById(attrValues, attrValueId);
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // orders
    // =========================
    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> listOrders(HttpServletRequest request,
                                                       @RequestParam(defaultValue = "1") int pageNo,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false, name = "status_code") String statusCode,
                                                       @RequestParam(required = false, name = "procurement_status") String procurementStatus) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<Long, Map<String, Object>> deliveryByOrderId = orderDeliveries.stream()
                .collect(Collectors.toMap(
                        item -> InMemoryData.toLong(item.getOrDefault("order_id", 0)),
                        this::copyMap,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        Map<Long, List<Map<String, Object>>> itemsByOrderId = orderItems.stream()
                .collect(Collectors.groupingBy(
                        item -> InMemoryData.toLong(item.getOrDefault("order_id", 0)),
                        LinkedHashMap::new,
                        Collectors.mapping(this::copyMap, Collectors.toList())
                ));
        List<Map<String, Object>> source = orders.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "order_no", "user_name", "remark", "id"))
                .filter(item -> !StringUtils.hasText(statusCode) || Objects.equals(statusCode, item.get("order_status_code")))
                .filter(item -> !StringUtils.hasText(procurementStatus) || matchesProcurementStatus(item, procurementStatus))
                .sorted((a, b) -> String.valueOf(b.get("submit_at")).compareTo(String.valueOf(a.get("submit_at"))))
                .map(item -> {
                    ensureOrderProcurementFields(item);
                    Map<String, Object> row = copyMap(item);
                    Long orderId = InMemoryData.toLong(row.getOrDefault("id", 0));
                    Map<String, Object> delivery = deliveryByOrderId.get(orderId);
                    List<Map<String, Object>> items = itemsByOrderId.getOrDefault(orderId, new ArrayList<>());

                    String buyerName = delivery == null ? "" : String.valueOf(delivery.getOrDefault("receiver_name", ""));
                    String buyerPhone = delivery == null ? "" : String.valueOf(delivery.getOrDefault("receiver_phone", ""));
                    String buyerDisplay = buildBuyerDisplay(buyerName, buyerPhone);
                    row.put("buyer_name", buyerName);
                    row.put("buyer_phone", buyerPhone);
                    row.put("buyer_display", buyerDisplay);

                    String goodsSummary = buildOrderGoodsSummary(items);
                    row.put("goods_summary", goodsSummary);
                    return row;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> orderDetail(HttpServletRequest request, @PathVariable Long orderId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<String, Object> target = findById(orders, orderId);
        if (target == null) return ApiResponse.fail(4041, "订单不存在");
        return ApiResponse.ok(copyMap(target));
    }

    @GetMapping("/orders/{orderId}/items")
    public ApiResponse<Map<String, Object>> orderItems(HttpServletRequest request, @PathVariable Long orderId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = orderItems.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", deduplicateOrderItems(source)));
    }

    @GetMapping("/orders/{orderId}/flows")
    public ApiResponse<Map<String, Object>> orderFlows(HttpServletRequest request, @PathVariable Long orderId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = orderFlows.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .sorted((a, b) -> String.valueOf(a.get("occurred_at")).compareTo(String.valueOf(b.get("occurred_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", source));
    }

    @GetMapping("/orders/{orderId}/delivery")
    public ApiResponse<Map<String, Object>> orderDelivery(HttpServletRequest request, @PathVariable Long orderId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<String, Object> target = orderDeliveries.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .findFirst().orElse(null);
        return ApiResponse.ok(target == null ? null : copyMap(target));
    }

    public synchronized Map<String, Object> queryOrderLogisticsForApp(Long orderId) {
        Map<String, Object> delivery = orderDeliveries.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .findFirst()
                .orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        if (delivery == null) {
            result.put("traces", new ArrayList<>());
            result.put("trace_source", "EMPTY");
            return result;
        }

        String expressNo = String.valueOf(delivery.getOrDefault("express_no", "")).trim();
        String shipperCode = String.valueOf(delivery.getOrDefault("shipper_code", "")).trim();
        String expressCompany = String.valueOf(delivery.getOrDefault("express_company", "")).trim();
        String shipperPhone = String.valueOf(delivery.getOrDefault("shipper_phone", "")).trim();
        String statusCode = String.valueOf(delivery.getOrDefault("delivery_status_code", "")).trim();
        String statusText = String.valueOf(delivery.getOrDefault("delivery_status_text", "")).trim();
        String latestTime = String.valueOf(delivery.getOrDefault("latest_trace_time", "")).trim();
        String latestStation = String.valueOf(delivery.getOrDefault("latest_trace_station", "")).trim();
        String signedAt = String.valueOf(delivery.getOrDefault("signed_at", "")).trim();

        List<Map<String, Object>> traces = readStoredTraceList(delivery);
        String traceSource = traces.isEmpty() ? "LOCAL_EMPTY" : "LOCAL_CACHE";
        boolean touched = false;

        if (kdniaoLogisticsService != null
                && kdniaoLogisticsService.isConfigured()
                && !StringUtils.hasText(shipperCode)
                && StringUtils.hasText(expressNo)) {
            List<KdniaoLogisticsService.ShipperCandidate> detected = kdniaoLogisticsService.detectShippers(expressNo);
            if (!detected.isEmpty()) {
                shipperCode = detected.get(0).getCode();
                delivery.put("shipper_code", shipperCode);
                if (!StringUtils.hasText(expressCompany)) {
                    expressCompany = detected.get(0).getName();
                    delivery.put("express_company", expressCompany);
                }
                touched = true;
            }
        }

        if (kdniaoLogisticsService != null && kdniaoLogisticsService.isConfigured() && StringUtils.hasText(expressNo)) {
            KdniaoLogisticsService.QueryTraceResult query = kdniaoLogisticsService.queryTrace(shipperCode, expressNo, findOrderNo(orderId));
            result.put("query_success", query.isSuccess());
            result.put("query_message", query.getReason());
            result.put("query_request_type", query.getRequestType());

            if (query.isSuccess()) {
                if (StringUtils.hasText(query.getShipperCode()) && !Objects.equals(shipperCode, query.getShipperCode())) {
                    shipperCode = query.getShipperCode();
                    delivery.put("shipper_code", shipperCode);
                    touched = true;
                }
                if (StringUtils.hasText(query.getShipperName()) && !Objects.equals(expressCompany, query.getShipperName())) {
                    expressCompany = query.getShipperName();
                    delivery.put("express_company", expressCompany);
                    touched = true;
                }
                if (StringUtils.hasText(query.getShipperPhone()) && !Objects.equals(shipperPhone, query.getShipperPhone())) {
                    shipperPhone = query.getShipperPhone();
                    delivery.put("shipper_phone", shipperPhone);
                    touched = true;
                }
                String queriedStatusCode = kdniaoLogisticsService.mapStateToDeliveryStatus(query.getState());
                String queriedStatusText = StringUtils.hasText(query.getStatusText())
                        ? query.getStatusText()
                        : kdniaoLogisticsService.mapStateToText(query.getState());
                if (StringUtils.hasText(queriedStatusCode) && !Objects.equals(statusCode, queriedStatusCode)) {
                    statusCode = queriedStatusCode;
                    delivery.put("delivery_status_code", queriedStatusCode);
                    touched = true;
                }
                if (StringUtils.hasText(queriedStatusText) && !Objects.equals(statusText, queriedStatusText)) {
                    statusText = queriedStatusText;
                    delivery.put("delivery_status_text", queriedStatusText);
                    touched = true;
                }

                List<Map<String, Object>> liveTraces = convertTraceNodesToList(query.getTraces());
                if (!liveTraces.isEmpty()) {
                    traces = liveTraces;
                    traceSource = "KDNIAO_LIVE";
                    delivery.put("trace_list", liveTraces);
                    latestTime = String.valueOf(liveTraces.get(0).getOrDefault("accept_time", latestTime));
                    latestStation = String.valueOf(liveTraces.get(0).getOrDefault("accept_station", latestStation));
                    delivery.put("latest_trace_time", latestTime);
                    delivery.put("latest_trace_station", latestStation);
                    touched = true;
                }
                if ("SIGNED".equalsIgnoreCase(statusCode) && !StringUtils.hasText(signedAt)) {
                    signedAt = StringUtils.hasText(latestTime) ? latestTime : now();
                    delivery.put("signed_at", signedAt);
                    touched = true;
                }
            }
        }

        if (traces.isEmpty() && StringUtils.hasText(latestTime)) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("accept_time", latestTime);
            one.put("accept_station", StringUtils.hasText(latestStation) ? latestStation : "物流状态已更新");
            one.put("status", statusText);
            traces.add(one);
            traceSource = "LATEST_FALLBACK";
        }

        if (touched) {
            syncOrderToMiniProgram(orderId);
            persistStateToMysql();
        }

        result.put("company_name", expressCompany);
        result.put("express_no", expressNo);
        result.put("shipper_code", shipperCode);
        result.put("shipper_phone", shipperPhone);
        result.put("delivery_status_code", statusCode);
        result.put("delivery_status_text", statusText);
        result.put("latest_trace_time", latestTime);
        result.put("latest_trace_station", latestStation);
        result.put("ship_at", String.valueOf(delivery.getOrDefault("ship_at", "")));
        result.put("signed_at", String.valueOf(delivery.getOrDefault("signed_at", "")));
        result.put("receiver_address", String.valueOf(delivery.getOrDefault("receiver_address", "")));
        result.put("provider_tip", "本数据由快递公司提供");
        result.put("trace_source", traceSource);
        result.put("traces", traces);
        return result;
    }

    @PostMapping("/logistics/detect")
    public ApiResponse<Map<String, Object>> detectLogistics(HttpServletRequest request,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        String expressNo = String.valueOf(payload == null ? "" : payload.getOrDefault("express_no", "")).trim();
        if (!StringUtils.hasText(expressNo)) {
            return ApiResponse.fail(4001, "快递单号不能为空");
        }
        if (kdniaoLogisticsService == null || !kdniaoLogisticsService.isConfigured()) {
            return ApiResponse.fail(4002, "快递鸟未配置或未启用");
        }
        List<KdniaoLogisticsService.ShipperCandidate> detected = kdniaoLogisticsService.detectShippers(expressNo);
        List<Map<String, Object>> list = detected.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("shipper_code", item.getCode());
            row.put("express_company", item.getName());
            row.put("score", item.getScore());
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        if (!list.isEmpty()) {
            data.put("auto_selected", list.get(0));
        }
        return ApiResponse.ok(data);
    }

    @PostMapping({"/logistics/kdniao/callback", "/logistics/kdniao", "/logistics/kdniao/"})
    public Map<String, Object> handleKdniaoCallback(@RequestParam(value = "RequestData", required = false) String requestData,
                                                     @RequestParam(value = "DataSign", required = false) String dataSign,
                                                     @RequestParam(value = "EBusinessID", required = false) String ebusinessId) {
        if (kdniaoLogisticsService == null || !kdniaoLogisticsService.isConfigured()) {
            return buildKdniaoCallbackAck(false, "快递服务未启用");
        }
        if (!StringUtils.hasText(requestData) || !StringUtils.hasText(dataSign)) {
            return buildKdniaoCallbackAck(false, "RequestData/DataSign 不能为空");
        }
        if (!kdniaoLogisticsService.verifySign(requestData, dataSign)) {
            // 快递鸟后台“校验格式”阶段可能使用演示签名；为保证回调地址校验可通过，这里统一返回成功应答。
            // 真正轨迹更新仅在签名通过时才会执行 applyKdniaoCallback。
            log.warn("快递鸟回调签名校验失败，已返回成功应答用于平台校验通过");
            return buildKdniaoCallbackAck(true, "接收成功");
        }
        if (StringUtils.hasText(ebusinessId) && !Objects.equals(ebusinessId.trim(), kdniaoLogisticsService.getBusinessId().trim())) {
            return buildKdniaoCallbackAck(false, "EBusinessID 不匹配");
        }

        int updated = applyKdniaoCallback(requestData);
        return buildKdniaoCallbackAck(true, updated > 0 ? "OK" : "IGNORE");
    }

    @PostMapping("/orders/{orderId}/approve")
    public ApiResponse<Map<String, Object>> approveOrder(HttpServletRequest request,
                                                         @PathVariable Long orderId,
                                                         @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        if (!changeOrderStatus(orderId, "PENDING_AUDIT", "PENDING_SHIP", "审核通过",
                String.valueOf(payload == null ? "" : payload.getOrDefault("audit_remark", "")))) {
            return ApiResponse.fail(4001, "当前状态不允许审核通过");
        }
        Map<String, Object> order = findById(orders, orderId);
        if (order != null) {
            order.put("buyer_decision_required", false);
        }
        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/orders/{orderId}/reject")
    public ApiResponse<Map<String, Object>> rejectOrder(HttpServletRequest request,
                                                        @PathVariable Long orderId,
                                                        @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        String reason = String.valueOf(payload == null ? "" : payload.getOrDefault("reject_reason", ""));
        Map<String, Object> currentOrder = findById(orders, orderId);
        if (currentOrder == null) return ApiResponse.fail(4041, "订单不存在");
        String currentStatus = String.valueOf(currentOrder.getOrDefault("order_status_code", ""));
        // 驳回入口已调整到「待发货」阶段；仅保留待发货可驳回
        if (!"PENDING_SHIP".equals(currentStatus)) {
            return ApiResponse.fail(4001, "仅待发货订单允许驳回");
        }
        if (!changeOrderStatus(orderId, currentStatus, "REJECTED", "发货驳回", reason)) {
            return ApiResponse.fail(4001, "当前状态不允许驳回");
        }

        Map<String, Object> order = findById(orders, orderId);
        if (order != null) {
            boolean refundImmediately = Boolean.TRUE.equals(payload == null ? Boolean.FALSE : payload.getOrDefault("refund_point", Boolean.FALSE));
            order.put("buyer_decision_required", !refundImmediately);
            order.put("reject_reason", reason);
            if (StringUtils.hasText(reason)) {
                order.put("admin_remark", reason);
            }
            if (refundImmediately) {
                refundOrderPoints(orderId, "ORDER_REJECT_REFUND", "驳回订单返还碎片");
            }
        }
        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/orders/{orderId}/ship")
    public ApiResponse<Map<String, Object>> shipOrder(HttpServletRequest request,
                                                      @PathVariable Long orderId,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        String company = String.valueOf(payload == null ? "" : payload.getOrDefault("express_company", "")).trim();
        String no = String.valueOf(payload == null ? "" : payload.getOrDefault("express_no", "")).trim();
        String shipperCode = String.valueOf(payload == null ? "" : payload.getOrDefault("shipper_code", "")).trim();
        if (!StringUtils.hasText(no)) {
            return ApiResponse.fail(4001, "请填写快递单号");
        }
        if ((!StringUtils.hasText(company) || !StringUtils.hasText(shipperCode))
                && kdniaoLogisticsService != null
                && kdniaoLogisticsService.isConfigured()) {
            List<KdniaoLogisticsService.ShipperCandidate> detected = kdniaoLogisticsService.detectShippers(no);
            if (!detected.isEmpty()) {
                KdniaoLogisticsService.ShipperCandidate best = detected.get(0);
                if (!StringUtils.hasText(shipperCode)) shipperCode = best.getCode();
                if (!StringUtils.hasText(company)) company = best.getName();
            }
        }
        if (!StringUtils.hasText(company) && StringUtils.hasText(shipperCode)) {
            company = shipperCode;
        }
        if (!StringUtils.hasText(company)) {
            return ApiResponse.fail(4002, "无法识别物流公司，请手动填写物流公司");
        }

        if (!changeOrderStatus(orderId, "PENDING_SHIP", "SHIPPED", "订单发货", company + " " + no)) {
            return ApiResponse.fail(4001, "当前状态不允许发货");
        }

        Map<String, Object> delivery = orderDeliveries.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .findFirst().orElse(null);
        if (delivery == null) {
            delivery = new LinkedHashMap<>();
            delivery.put("order_id", orderId);
            delivery.put("receiver_name", "收件人");
            delivery.put("receiver_phone", "13800000000");
            delivery.put("receiver_address", "上海市浦东新区世纪大道100号");
            orderDeliveries.add(delivery);
        }
        delivery.put("express_company", company);
        delivery.put("express_no", no);
        delivery.put("shipper_code", shipperCode);
        delivery.put("ship_at", now());
        delivery.put("signed_at", "");
        delivery.put("delivery_status_code", "IN_TRANSIT");
        delivery.put("delivery_status_text", "在途中");
        Map<String, Object> initTrace = new LinkedHashMap<>();
        initTrace.put("accept_time", now());
        initTrace.put("accept_station", "快件已发出");
        initTrace.put("status", "已发货");
        delivery.put("trace_list", new ArrayList<>(Collections.singletonList(initTrace)));

        boolean subscribed = false;
        String subscribeMsg = "";
        if (kdniaoLogisticsService != null && kdniaoLogisticsService.isConfigured() && StringUtils.hasText(shipperCode)) {
            KdniaoLogisticsService.SubscribeResult subscribeResult =
                    kdniaoLogisticsService.subscribeTrace(shipperCode, no, String.valueOf(findOrderNo(orderId)));
            subscribed = subscribeResult.isSuccess();
            subscribeMsg = subscribeResult.getMessage();
            delivery.put("trace_subscribed", subscribed);
            delivery.put("trace_subscribe_message", subscribeMsg);
        } else {
            delivery.put("trace_subscribed", false);
            delivery.put("trace_subscribe_message", StringUtils.hasText(shipperCode) ? "快递鸟未启用" : "缺少物流公司编码");
        }

        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("express_company", company);
        result.put("express_no", no);
        result.put("shipper_code", shipperCode);
        result.put("trace_subscribed", subscribed);
        result.put("trace_subscribe_message", subscribeMsg);
        return ApiResponse.ok(result);
    }

    @PutMapping("/orders/{orderId}/procurement")
    public ApiResponse<Map<String, Object>> updateOrderProcurement(HttpServletRequest request,
                                                                   @PathVariable Long orderId,
                                                                   @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return ApiResponse.fail(4041, "订单不存在");
        String orderStatus = String.valueOf(order.getOrDefault("order_status_code", ""));
        if (!("PENDING_SHIP".equalsIgnoreCase(orderStatus) || "SHIPPED".equalsIgnoreCase(orderStatus))) {
            return ApiResponse.fail(4001, "仅待发货/已发货订单允许操作采购状态");
        }

        ensureOrderProcurementFields(order);
        String current = String.valueOf(order.getOrDefault("procurement_status", "PENDING_PROCURE"));
        String target = normalizeProcurementStatus(payload == null ? "" : String.valueOf(payload.getOrDefault("procurement_status", "")));
        if (!StringUtils.hasText(target)) {
            target = "PROCURED".equalsIgnoreCase(current) ? "PENDING_PROCURE" : "PROCURED";
        }
        if (Objects.equals(current, target)) {
            return ApiResponse.ok(Map.of(
                    "success", true,
                    "procurement_status", current,
                    "procurement_status_text", procurementStatusText(current),
                    "procured_at", String.valueOf(order.getOrDefault("procured_at", ""))
            ));
        }

        order.put("procurement_status", target);
        order.put("procurement_status_text", procurementStatusText(target));
        order.put("_order_admin_touched", true);
        if ("PROCURED".equalsIgnoreCase(target)) {
            order.put("procured_at", now());
            order.put("procured_by", "系统管理员");
        } else {
            order.put("procured_at", "");
            order.put("procured_by", "");
        }
        Map<String, Object> flow = orderFlow(orderFlowIdSeq.incrementAndGet(), orderId, orderStatus, orderStatus,
                "PROCURED".equalsIgnoreCase(target) ? "采购完成" : "重置待采购",
                "PROCURED".equalsIgnoreCase(target) ? "该订单已采购" : "该订单已恢复待采购",
                "系统管理员", now());
        orderFlows.add(flow);
        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("procurement_status", target);
        data.put("procurement_status_text", procurementStatusText(target));
        data.put("procured_at", String.valueOf(order.getOrDefault("procured_at", "")));
        return ApiResponse.ok(data);
    }

    @PostMapping("/orders/{orderId}/complete")
    public ApiResponse<Map<String, Object>> completeOrder(HttpServletRequest request, @PathVariable Long orderId) {
        requireAdminLike(request);
        if (!changeOrderStatus(orderId, "SHIPPED", "FINISHED", "订单完成", "")) {
            return ApiResponse.fail(4001, "当前状态不允许完成");
        }
        Map<String, Object> order = findById(orders, orderId);
        if (order != null) {
            order.put("buyer_decision_required", false);
        }
        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/orders/{orderId}/close")
    public ApiResponse<Map<String, Object>> closeOrder(HttpServletRequest request,
                                                       @PathVariable Long orderId,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return ApiResponse.fail(4041, "订单不存在");
        String current = String.valueOf(order.get("order_status_code"));
        if (!("PENDING_AUDIT".equals(current) || "PENDING_SHIP".equals(current) || "SHIPPED".equals(current))) {
            return ApiResponse.fail(4001, "当前状态不允许关闭");
        }
        changeOrderStatus(orderId, current, "CLOSED", "订单关闭", String.valueOf(payload == null ? "" : payload.getOrDefault("close_reason", "")));
        Map<String, Object> latestOrder = findById(orders, orderId);
        if (latestOrder != null) {
            latestOrder.put("buyer_decision_required", false);
        }
        refundOrderPoints(orderId, "ORDER_CLOSE_REFUND", "订单关闭返还碎片");
        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/orders/{orderId}/remark")
    public ApiResponse<Map<String, Object>> updateOrderRemark(HttpServletRequest request,
                                                              @PathVariable Long orderId,
                                                              @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return ApiResponse.fail(4041, "订单不存在");
        order.put("remark", String.valueOf(payload == null ? "" : payload.getOrDefault("remark", "")));
        order.put("_order_admin_touched", true);
        syncOrderToMiniProgram(orderId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> deleteOrder(HttpServletRequest request, @PathVariable Long orderId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(orders, orderId);
        if (target == null) return ApiResponse.fail(4041, "订单不存在");
        String status = String.valueOf(target.getOrDefault("order_status_code", ""));
        if (!"CLOSED".equalsIgnoreCase(status)) {
            return ApiResponse.fail(4001, "仅已关闭订单允许删除");
        }
        removeById(orders, orderId);
        orderItems.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId));
        orderFlows.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId));
        orderDeliveries.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId));
        if (appStore != null) {
            appStore.removeOrderById(orderId);
        }
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/orders/procurement-export")
    public ApiResponse<Map<String, Object>> procurementExport(HttpServletRequest request,
                                                              @RequestParam(required = false, name = "status_code") String statusCode,
                                                              @RequestParam(required = false, name = "submit_date") String submitDate,
                                                              @RequestParam(required = false, name = "procurement_status") String procurementStatus) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> rows = buildProcurementExportRows(statusCode, submitDate, procurementStatus);
        return ApiResponse.ok(Map.of(
                "total", rows.size(),
                "list", rows
        ));
    }

    @GetMapping("/orders/procurement-export/csv")
    public ResponseEntity<byte[]> procurementExportCsv(HttpServletRequest request,
                                                       @RequestParam(required = false, name = "status_code") String statusCode,
                                                       @RequestParam(required = false, name = "submit_date") String submitDate,
                                                       @RequestParam(required = false, name = "procurement_status") String procurementStatus) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> rows = buildProcurementExportRows(statusCode, submitDate, procurementStatus);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("订单号,用户ID,订单状态,采购状态,商品+SKU+数量,商品碎片价,用户备注,买家信息\n");
        for (Map<String, Object> row : rows) {
            sb.append(csvCell(row.get("order_no"))).append(',')
                    .append(csvCell(row.get("user_id"))).append(',')
                    .append(csvCell(row.get("order_status_text"))).append(',')
                    .append(csvCell(row.get("procurement_status_text"))).append(',')
                    .append(csvCell(row.get("product_sku_qty"))).append(',')
                    .append(csvCell(row.get("point_price"))).append(',')
                    .append(csvCell(row.get("user_remark"))).append(',')
                    .append(csvCell(row.get("buyer_full_info")))
                    .append('\n');
        }
        String fileName = "procurement-orders-" + LocalDate.now(BEIJING_ZONE).format(FILE_DAY) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // =========================
    // wish demands
    // =========================
    @GetMapping("/wish-demands")
    public ApiResponse<Map<String, Object>> listWishDemands(HttpServletRequest request,
                                                            @RequestParam(defaultValue = "1") int pageNo,
                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false, name = "status_code") String statusCode) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeStatus = statusCode == null ? "" : statusCode.trim().toUpperCase(Locale.ROOT);
        List<Map<String, Object>> source = wishDemands.stream()
                .filter(item -> !StringUtils.hasText(safeKeyword) || matchKeyword(item, safeKeyword, "id", "user_id", "user_name", "wish_title", "wish_message"))
                .filter(item -> !StringUtils.hasText(safeStatus) || safeStatus.equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", ""))))
                .sorted((a, b) -> String.valueOf(b.getOrDefault("created_at", "")).compareTo(String.valueOf(a.getOrDefault("created_at", ""))))
                .map(this::copyMap)
                .map(this::normalizeWishDemandImageFields)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PutMapping("/wish-demands/{wishId}/decision")
    public ApiResponse<Map<String, Object>> decideWishDemand(HttpServletRequest request,
                                                             @PathVariable Long wishId,
                                                             @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        String decision = payload == null ? "" : String.valueOf(payload.getOrDefault("decision", ""));
        String note = payload == null ? "" : String.valueOf(payload.getOrDefault("decision_note", payload.getOrDefault("note", "")));
        String operatorName = "管理员";
        Map<String, Object> updated = decideWishDemandInternal(wishId, decision, note, operatorName);
        if (updated == null) return ApiResponse.fail(4041, "留言不存在或已处理");
        persistStateToMysql();
        return ApiResponse.ok(normalizeWishDemandImageFields(updated));
    }

    public synchronized Map<String, Object> createWishDemandFromApp(Long userId, Map<String, Object> payload) {
        syncFromMiniProgramCoreData();
        Map<String, Object> user = findById(users, userId);
        if (user == null) return null;

        String wishTitle = String.valueOf(payload == null ? "" : payload.getOrDefault("wish_title", payload.getOrDefault("product_name", ""))).trim();
        String wishMessage = String.valueOf(payload == null ? "" : payload.getOrDefault("wish_message", payload.getOrDefault("remark", ""))).trim();
        if (!StringUtils.hasText(wishTitle) && !StringUtils.hasText(wishMessage)) {
            return null;
        }

        List<String> normalizedImages = normalizeWishDemandPayloadImages(payload);
        String normalizedImage = normalizedImages.isEmpty() ? "" : normalizedImages.get(0);

        Map<String, Object> item = new LinkedHashMap<>();
        long id = wishDemandIdSeq.incrementAndGet();
        item.put("id", id);
        item.put("user_id", userId);
        item.put("user_name", String.valueOf(user.getOrDefault("nick_name", "用户")));
        item.put("phone_masked", String.valueOf(user.getOrDefault("phone_masked", "")));
        item.put("wish_title", wishTitle);
        item.put("wish_message", wishMessage);
        item.put("image_url", normalizedImage);
        item.put("image_urls", normalizedImages);
        item.put("status_code", "PENDING");
        item.put("status_text", wishDemandStatusText("PENDING"));
        item.put("decision_note", "");
        item.put("notify_content", "我们已收到你的上架意向，正在审核中");
        item.put("created_at", now());
        item.put("updated_at", now());
        item.put("decided_at", "");
        item.put("decided_by", "");
        wishDemands.add(0, item);
        persistStateToMysql();
        return copyMap(item);
    }

    public synchronized Map<String, Object> listWishDemandsForApp(Long userId, int pageNo, int pageSize) {
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = wishDemands.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.getOrDefault("user_id", 0)), userId))
                .sorted((a, b) -> String.valueOf(b.getOrDefault("created_at", "")).compareTo(String.valueOf(a.getOrDefault("created_at", ""))))
                .map(this::copyMap)
                .map(this::normalizeWishDemandImageFields)
                .collect(Collectors.toList());
        return pageResult(source, pageNo, pageSize);
    }

    private List<String> normalizeWishDemandPayloadImages(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        candidates.addAll(extractWishImageStrings(payload.get("image_data_urls")));
        candidates.addAll(extractWishImageStrings(payload.get("image_urls")));
        candidates.addAll(extractWishImageStrings(payload.get("image_data_url")));
        candidates.addAll(extractWishImageStrings(payload.get("image_url")));

        List<String> normalized = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (normalized.size() >= MAX_WISH_DEMAND_IMAGES) break;
            String url = normalizePublicImageUrl(candidate);
            if (!StringUtils.hasText(url) || !dedup.add(url)) continue;
            normalized.add(url);
        }
        return normalized;
    }

    private Map<String, Object> normalizeWishDemandImageFields(Map<String, Object> row) {
        if (row == null) return new LinkedHashMap<>();
        List<String> imageUrls = extractWishImageUrlsFromStoredRow(row);
        row.put("image_urls", imageUrls);
        row.put("image_url", imageUrls.isEmpty() ? "" : imageUrls.get(0));
        return row;
    }

    private List<String> extractWishImageUrlsFromStoredRow(Map<String, Object> row) {
        List<String> candidates = new ArrayList<>();
        candidates.addAll(extractWishImageStrings(row.get("image_urls")));
        candidates.addAll(extractWishImageStrings(row.get("image_url")));
        List<String> result = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (result.size() >= MAX_WISH_DEMAND_IMAGES) break;
            String text = String.valueOf(candidate == null ? "" : candidate).trim();
            if (!StringUtils.hasText(text) || !dedup.add(text)) continue;
            result.add(text);
        }
        return result;
    }

    private List<String> extractWishImageStrings(Object rawValue) {
        List<String> result = new ArrayList<>();
        if (rawValue == null) return result;
        if (rawValue instanceof Collection<?>) {
            for (Object item : (Collection<?>) rawValue) {
                String text = String.valueOf(item == null ? "" : item).trim();
                if (StringUtils.hasText(text)) result.add(text);
            }
            return result;
        }
        if (rawValue.getClass().isArray()) {
            int length = Array.getLength(rawValue);
            for (int i = 0; i < length; i++) {
                String text = String.valueOf(Array.get(rawValue, i)).trim();
                if (StringUtils.hasText(text)) result.add(text);
            }
            return result;
        }
        String text = String.valueOf(rawValue).trim();
        if (!StringUtils.hasText(text)) return result;
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                List<Object> parsed = objectMapper.readValue(text, new TypeReference<List<Object>>() {});
                for (Object item : parsed) {
                    String value = String.valueOf(item == null ? "" : item).trim();
                    if (StringUtils.hasText(value)) result.add(value);
                }
                if (!result.isEmpty()) return result;
            } catch (Exception ignore) {
            }
        }
        result.add(text);
        return result;
    }

    // =========================
    // backpack
    // =========================
    @GetMapping("/backpack/assets")
    public ApiResponse<Map<String, Object>> listBackpackAssets(HttpServletRequest request,
                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false, name = "status_code") String statusCode) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = backpackAssets.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "asset_no", "user_name", "asset_name"))
                .filter(item -> !StringUtils.hasText(statusCode) || Objects.equals(statusCode, item.get("status_code")))
                .sorted((a, b) -> String.valueOf(b.get("obtained_at")).compareTo(String.valueOf(a.get("obtained_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @GetMapping("/backpack/assets/{assetId}")
    public ApiResponse<Map<String, Object>> backpackAssetDetail(HttpServletRequest request, @PathVariable Long assetId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<String, Object> target = findById(backpackAssets, assetId);
        if (target == null) return ApiResponse.fail(4041, "资产不存在");
        return ApiResponse.ok(copyMap(target));
    }

    @GetMapping("/backpack/asset-flows")
    public ApiResponse<Map<String, Object>> listBackpackFlows(HttpServletRequest request,
                                                              @RequestParam(required = false, name = "asset_id") Long assetId,
                                                              @RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "50") int pageSize) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = backpackFlows.stream()
                .filter(item -> assetId == null || Objects.equals(InMemoryData.toLong(item.get("asset_id")), assetId))
                .sorted((a, b) -> String.valueOf(b.get("occurred_at")).compareTo(String.valueOf(a.get("occurred_at"))))
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/backpack/assets/grant")
    public ApiResponse<Map<String, Object>> grantBackpack(HttpServletRequest request,
                                                          @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        long id = assetIdSeq.incrementAndGet();
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("id", id);
        asset.put("asset_no", "BA" + LocalDate.now(BEIJING_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + id);
        asset.put("user_name", String.valueOf(payload == null ? "未知用户" : payload.getOrDefault("user_name", "未知用户")));
        asset.put("asset_name", String.valueOf(payload == null ? "新资产" : payload.getOrDefault("asset_name", "新资产")));
        asset.put("asset_type_code", String.valueOf(payload == null ? "GROUP_QR" : payload.getOrDefault("asset_type_code", "GROUP_QR")));
        asset.put("status_code", "ACTIVE");
        asset.put("obtained_at", now());
        asset.put("expire_at", String.valueOf(payload == null ? "2099-12-31 23:59:59" : payload.getOrDefault("expire_at", "2099-12-31 23:59:59")));
        backpackAssets.add(0, asset);

        appendAssetFlow(id, "GRANT", "发放资产", "后台手工发放");
        return ApiResponse.ok(copyMap(asset));
    }

    @PostMapping("/backpack/assets/{assetId}/invalidate")
    public ApiResponse<Map<String, Object>> invalidateBackpack(HttpServletRequest request,
                                                               @PathVariable Long assetId,
                                                               @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(backpackAssets, assetId);
        if (target == null) return ApiResponse.fail(4041, "资产不存在");
        target.put("status_code", "INVALID");
        appendAssetFlow(assetId, "INVALIDATE", "资产失效", String.valueOf(payload == null ? "" : payload.getOrDefault("reason", "")));
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/backpack/assets/{assetId}/expire")
    public ApiResponse<Map<String, Object>> expireBackpack(HttpServletRequest request,
                                                           @PathVariable Long assetId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(backpackAssets, assetId);
        if (target == null) return ApiResponse.fail(4041, "资产不存在");
        target.put("status_code", "EXPIRED");
        appendAssetFlow(assetId, "EXPIRE", "资产过期", String.valueOf(payload == null ? "" : payload.getOrDefault("reason", "")));
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // group resources
    // =========================
    @GetMapping("/group-resources")
    public ApiResponse<Map<String, Object>> listGroupResources(HttpServletRequest request,
                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                               @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        List<Map<String, Object>> source = groupResources.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "group_name", "intro_text"))
                .sorted((a, b) -> String.valueOf(b.get("updated_at")).compareTo(String.valueOf(a.get("updated_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/group-resources")
    public ApiResponse<Map<String, Object>> createGroupResource(HttpServletRequest request,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        Long resourceId = groupResourceIdSeq.incrementAndGet();
        item.put("id", resourceId);
        item.put("group_name", String.valueOf(payload == null ? "新群资源" : payload.getOrDefault("group_name", "新群资源")));
        item.put("qr_image_url", String.valueOf(payload == null ? "" : payload.getOrDefault("qr_image_url", "")));
        item.put("intro_text", String.valueOf(payload == null ? "" : payload.getOrDefault("intro_text", "")));
        item.put("max_member_count", InMemoryData.toInt(payload == null ? 500 : payload.getOrDefault("max_member_count", 500)));
        item.put("current_member_count", InMemoryData.toInt(payload == null ? 0 : payload.getOrDefault("current_member_count", 0)));
        item.put("expire_at", String.valueOf(payload == null ? "2099-12-31 23:59:59" : payload.getOrDefault("expire_at", "2099-12-31 23:59:59")));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("updated_at", now());
        item.put("_sync_locked", true);
        groupResourceDeletedIds.remove(resourceId);
        groupResources.add(0, item);
        if (appStore != null) {
            appStore.upsertGroupResourceFromAdmin(
                    resourceId,
                    String.valueOf(item.getOrDefault("group_name", "")),
                    mapGroupStatusToApp(String.valueOf(item.getOrDefault("status_code", "ENABLED"))),
                    String.valueOf(item.getOrDefault("qr_image_url", ""))
            );
        }
        return ApiResponse.ok(copyMap(item));
    }

    @GetMapping("/group-resources/{resourceId}")
    public ApiResponse<Map<String, Object>> groupResourceDetail(HttpServletRequest request, @PathVariable Long resourceId) {
        requireAdminLike(request);
        syncFromMiniProgramCoreData();
        Map<String, Object> target = findById(groupResources, resourceId);
        if (target == null) return ApiResponse.fail(4041, "群资源不存在");
        return ApiResponse.ok(copyMap(target));
    }

    @PutMapping("/group-resources/{resourceId}")
    public ApiResponse<Map<String, Object>> updateGroupResource(HttpServletRequest request,
                                                                @PathVariable Long resourceId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(groupResources, resourceId);
        if (target == null) return ApiResponse.fail(4041, "群资源不存在");
        mergeFields(target, payload, "group_name", "qr_image_url", "intro_text", "max_member_count", "current_member_count", "expire_at", "status_code");
        target.put("updated_at", now());
        target.put("_sync_locked", true);
        groupResourceDeletedIds.remove(resourceId);
        if (appStore != null) {
            appStore.upsertGroupResourceFromAdmin(
                    resourceId,
                    String.valueOf(target.getOrDefault("group_name", "")),
                    mapGroupStatusToApp(String.valueOf(target.getOrDefault("status_code", "ENABLED"))),
                    String.valueOf(target.getOrDefault("qr_image_url", ""))
            );
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/group-resources/{resourceId}/status")
    public ApiResponse<Map<String, Object>> groupResourceStatus(HttpServletRequest request,
                                                                @PathVariable Long resourceId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(groupResources, resourceId);
        if (target == null) return ApiResponse.fail(4041, "群资源不存在");
        target.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        target.put("updated_at", now());
        target.put("_sync_locked", true);
        groupResourceDeletedIds.remove(resourceId);
        if (appStore != null) {
            appStore.upsertGroupResourceFromAdmin(
                    resourceId,
                    String.valueOf(target.getOrDefault("group_name", "")),
                    mapGroupStatusToApp(String.valueOf(target.getOrDefault("status_code", "ENABLED"))),
                    String.valueOf(target.getOrDefault("qr_image_url", ""))
            );
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/group-resources/{resourceId}")
    public ApiResponse<Map<String, Object>> deleteGroupResource(HttpServletRequest request, @PathVariable Long resourceId) {
        requireAdminLike(request);
        removeById(groupResources, resourceId);
        groupResourceDeletedIds.add(resourceId);
        if (appStore != null) {
            appStore.removeGroupResourceById(resourceId);
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // dict / configs / files
    // =========================
    @GetMapping("/dict/types")
    public ApiResponse<Map<String, Object>> listDictTypes(HttpServletRequest request,
                                                          @RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "20") int pageSize,
                                                          @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = dictTypes.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "dict_type_code", "dict_type_name", "remark"))
                .sorted((a, b) -> String.valueOf(b.get("updated_at")).compareTo(String.valueOf(a.get("updated_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/dict/types")
    public ApiResponse<Map<String, Object>> createDictType(HttpServletRequest request,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", dictTypeIdSeq.incrementAndGet());
        item.put("dict_type_code", String.valueOf(payload == null ? "NEW_DICT" : payload.getOrDefault("dict_type_code", "NEW_DICT")));
        item.put("dict_type_name", String.valueOf(payload == null ? "新字典" : payload.getOrDefault("dict_type_name", "新字典")));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("remark", String.valueOf(payload == null ? "" : payload.getOrDefault("remark", "")));
        item.put("updated_at", now());
        dictTypes.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/dict/types/{typeId}")
    public ApiResponse<Map<String, Object>> updateDictType(HttpServletRequest request,
                                                           @PathVariable Long typeId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(dictTypes, typeId);
        if (target == null) return ApiResponse.fail(4041, "字典类型不存在");
        mergeFields(target, payload, "dict_type_code", "dict_type_name", "status_code", "remark");
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/dict/types/{typeId}")
    public ApiResponse<Map<String, Object>> deleteDictType(HttpServletRequest request, @PathVariable Long typeId) {
        requireAdminLike(request);
        Map<String, Object> removed = findById(dictTypes, typeId);
        if (removed != null) {
            String typeCode = String.valueOf(removed.get("dict_type_code"));
            dictItems.removeIf(item -> Objects.equals(String.valueOf(item.get("dict_type_code")), typeCode));
        }
        removeById(dictTypes, typeId);
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/dict/types/{typeCode}/items")
    public ApiResponse<Map<String, Object>> listDictItems(HttpServletRequest request, @PathVariable String typeCode) {
        requireAdminLike(request);
        List<Map<String, Object>> source = dictItems.stream()
                .filter(item -> Objects.equals(typeCode, String.valueOf(item.get("dict_type_code"))))
                .sorted((a, b) -> InMemoryData.toInt(b.get("sort_no")) - InMemoryData.toInt(a.get("sort_no")))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", source));
    }

    @PostMapping("/dict/types/{typeCode}/items")
    public ApiResponse<Map<String, Object>> createDictItem(HttpServletRequest request,
                                                           @PathVariable String typeCode,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", dictItemIdSeq.incrementAndGet());
        item.put("dict_type_code", typeCode);
        item.put("item_code", String.valueOf(payload == null ? "ITEM_NEW" : payload.getOrDefault("item_code", "ITEM_NEW")));
        item.put("item_name", String.valueOf(payload == null ? "新字典项" : payload.getOrDefault("item_name", "新字典项")));
        item.put("item_value", String.valueOf(payload == null ? "VALUE" : payload.getOrDefault("item_value", "VALUE")));
        item.put("sort_no", InMemoryData.toInt(payload == null ? 100 : payload.getOrDefault("sort_no", 100)));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("updated_at", now());
        dictItems.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/dict/items/{itemId}")
    public ApiResponse<Map<String, Object>> updateDictItem(HttpServletRequest request,
                                                           @PathVariable Long itemId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(dictItems, itemId);
        if (target == null) return ApiResponse.fail(4041, "字典项不存在");
        mergeFields(target, payload, "item_code", "item_name", "item_value", "sort_no", "status_code");
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/dict/items/{itemId}")
    public ApiResponse<Map<String, Object>> deleteDictItem(HttpServletRequest request, @PathVariable Long itemId) {
        requireAdminLike(request);
        removeById(dictItems, itemId);
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/system-configs")
    public ApiResponse<Map<String, Object>> listSystemConfigs(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize,
                                                              @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = systemConfigs.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "config_key", "config_name", "group_code", "remark"))
                .sorted((a, b) -> String.valueOf(b.get("updated_at")).compareTo(String.valueOf(a.get("updated_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/system-configs")
    public ApiResponse<Map<String, Object>> createSystemConfig(HttpServletRequest request,
                                                               @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", configIdSeq.incrementAndGet());
        item.put("config_key", String.valueOf(payload == null ? "mall.new.key" : payload.getOrDefault("config_key", "mall.new.key")));
        item.put("config_name", String.valueOf(payload == null ? "新配置" : payload.getOrDefault("config_name", "新配置")));
        item.put("config_value", String.valueOf(payload == null ? "" : payload.getOrDefault("config_value", "")));
        item.put("value_type_code", String.valueOf(payload == null ? "STRING" : payload.getOrDefault("value_type_code", "STRING")));
        item.put("group_code", String.valueOf(payload == null ? "DEFAULT" : payload.getOrDefault("group_code", "DEFAULT")));
        item.put("status_code", String.valueOf(payload == null ? "ENABLED" : payload.getOrDefault("status_code", "ENABLED")));
        item.put("remark", String.valueOf(payload == null ? "" : payload.getOrDefault("remark", "")));
        item.put("updated_at", now());
        systemConfigs.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/system-configs/{configId}")
    public ApiResponse<Map<String, Object>> updateSystemConfig(HttpServletRequest request,
                                                               @PathVariable Long configId,
                                                               @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(systemConfigs, configId);
        if (target == null) return ApiResponse.fail(4041, "配置不存在");
        mergeFields(target, payload, "config_key", "config_name", "config_value", "value_type_code", "group_code", "status_code", "remark");
        target.put("updated_at", now());
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/system-configs/{configId}")
    public ApiResponse<Map<String, Object>> deleteSystemConfig(HttpServletRequest request, @PathVariable Long configId) {
        requireAdminLike(request);
        removeById(systemConfigs, configId);
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/files")
    public ApiResponse<Map<String, Object>> listFiles(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = files.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "file_name", "file_url", "mime_type"))
                .sorted((a, b) -> String.valueOf(b.get("uploaded_at")).compareTo(String.valueOf(a.get("uploaded_at"))))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/files/upload")
    public ApiResponse<Map<String, Object>> uploadFile(HttpServletRequest request,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        long fileId = fileIdSeq.incrementAndGet();
        Map<String, Object> file = new LinkedHashMap<>();
        String fileName = String.valueOf(payload == null ? "new-file.png" : payload.getOrDefault("file_name", "new-file.png"));
        file.put("id", fileId);
        file.put("file_name", fileName);
        String rawFileUrl = String.valueOf(payload == null ? "" : payload.getOrDefault("file_url", ""));
        String mimeType = String.valueOf(payload == null ? "application/octet-stream" : payload.getOrDefault("mime_type", "application/octet-stream"));
        long fileSizeKb = InMemoryData.toLong(payload == null ? 0 : payload.getOrDefault("file_size_kb", 0));
        if (rawFileUrl.startsWith("data:")) {
            LocalStoredFile stored = storeDataUrlToLocalFile(fileId, fileName, rawFileUrl, mimeType);
            if (stored != null) {
                file.put("_file_local_path", stored.relativePath);
                file.put("file_url", buildFileContentUrl(fileId));
                mimeType = stored.mimeType;
                fileSizeKb = stored.fileSizeKb;
            } else {
                file.put("_file_data_url", rawFileUrl);
                file.put("file_url", buildFileContentPath(fileId));
                String dataMime = extractMimeTypeFromDataUrl(rawFileUrl);
                if (StringUtils.hasText(dataMime)) {
                    mimeType = dataMime;
                }
                fileSizeKb = Math.max(1, rawFileUrl.length() / 1024);
            }
        } else {
            file.put("file_url", rawFileUrl);
        }
        file.put("mime_type", mimeType);
        file.put("file_size_kb", fileSizeKb);
        file.put("uploaded_at", now());
        files.add(0, file);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("file", copyMap(file)));
    }

    @GetMapping("/files/{fileId}/content")
    public ResponseEntity<byte[]> fileContent(@PathVariable Long fileId,
                                              @RequestParam(value = "w", required = false) Integer width,
                                              @RequestParam(value = "q", required = false) Integer quality) {
        Map<String, Object> target = findById(files, fileId);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }
        String localPath = String.valueOf(target.getOrDefault("_file_local_path", ""));
        if (StringUtils.hasText(localPath)) {
            Path path = resolveExistingStoredFilePath(localPath);
            if (path != null && Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    byte[] bytes = Files.readAllBytes(path);
                    String mime = String.valueOf(target.getOrDefault("mime_type", ""));
                    if (!StringUtils.hasText(mime)) {
                        mime = Files.probeContentType(path);
                    }
                    if (!StringUtils.hasText(mime)) {
                        mime = guessMimeByFileName(
                                String.valueOf(target.getOrDefault("file_name", "")),
                                path.getFileName() == null ? "" : path.getFileName().toString()
                        );
                    }
                    byte[] transformed = maybeTransformImage(bytes, mime, width, quality);
                    return ResponseEntity.ok()
                            .contentType(safeParseMediaType(mime))
                            .header("Cache-Control", FILE_CACHE_CONTROL)
                            .body(transformed);
                } catch (Exception ex) {
                    log.warn("读取本地文件失败 fileId={}, path={}, err={}", fileId, path, ex.getMessage());
                }
            }
        }
        String dataUrl = String.valueOf(target.getOrDefault("_file_data_url", ""));
        if (!StringUtils.hasText(dataUrl)) {
            String raw = String.valueOf(target.getOrDefault("file_url", ""));
            if (raw.startsWith("data:")) {
                dataUrl = raw;
            }
        }
        if (!StringUtils.hasText(dataUrl) || !dataUrl.startsWith("data:")) {
            return ResponseEntity.notFound().build();
        }
        int split = dataUrl.indexOf(',');
        if (split < 0) {
            return ResponseEntity.notFound().build();
        }
        String base64Part = dataUrl.substring(split + 1);
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Part);
            String mime = extractMimeTypeFromDataUrl(dataUrl);
            if (!StringUtils.hasText(mime)) {
                mime = guessMimeByFileName(String.valueOf(target.getOrDefault("file_name", "")));
            }
            byte[] transformed = maybeTransformImage(bytes, mime, width, quality);
            return ResponseEntity.ok()
                    .contentType(safeParseMediaType(mime))
                    .header("Cache-Control", FILE_CACHE_CONTROL)
                    .body(transformed);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/files/{fileId}")
    public ApiResponse<Map<String, Object>> fileDetail(HttpServletRequest request, @PathVariable Long fileId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(files, fileId);
        if (target == null) return ApiResponse.fail(4041, "文件不存在");
        return ApiResponse.ok(copyMap(target));
    }

    @DeleteMapping("/files/{fileId}")
    public ApiResponse<Map<String, Object>> deleteFile(HttpServletRequest request, @PathVariable Long fileId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(files, fileId);
        if (target != null) {
            String localPath = String.valueOf(target.getOrDefault("_file_local_path", ""));
            Path path = resolveExistingStoredFilePath(localPath);
            if (path != null && Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ex) {
                    log.warn("删除本地文件失败 fileId={}, path={}, err={}", fileId, path, ex.getMessage());
                }
            }
        }
        removeById(files, fileId);
        persistStateToMysql();
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/reports/user-balances")
    public ApiResponse<Map<String, Object>> listUserBalanceReports(HttpServletRequest request) {
        requireAdminLike(request);
        if (userBalanceReportService == null) {
            return ApiResponse.ok(Map.of("list", new ArrayList<>(), "total", 0));
        }
        List<Map<String, Object>> list = userBalanceReportService.listReports();
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }

    @PostMapping("/reports/user-balances/export-today")
    public ApiResponse<Map<String, Object>> exportTodayUserBalance(HttpServletRequest request) {
        requireAdminLike(request);
        String fileName = generateDailyUserBalanceReport("MANUAL");
        return ApiResponse.ok(Map.of("success", true, "file_name", fileName == null ? "" : fileName));
    }

    @GetMapping("/reports/user-balances/{fileName}")
    public ResponseEntity<byte[]> downloadUserBalanceReport(HttpServletRequest request, @PathVariable String fileName) {
        requireAdminLike(request);
        if (userBalanceReportService == null || !StringUtils.hasText(fileName)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = userBalanceReportService.readReport(fileName);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .body(bytes);
    }

    // =========================
    // RBAC
    // =========================
    @GetMapping("/admin-users")
    public ApiResponse<Map<String, Object>> listAdminUsers(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize,
                                                           @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = adminUsers.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "username", "display_name", "phone"))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/admin-users")
    public ApiResponse<Map<String, Object>> createAdminUser(HttpServletRequest request,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        String username = String.valueOf(payload == null ? "new_admin" : payload.getOrDefault("username", "new_admin"));
        String defaultPwd = buildDefaultAdminPassword(username);
        String password = String.valueOf(payload == null ? defaultPwd : payload.getOrDefault("password", defaultPwd));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", adminUserIdSeq.incrementAndGet());
        item.put("username", username);
        item.put("display_name", String.valueOf(payload == null ? "新管理员" : payload.getOrDefault("display_name", "新管理员")));
        item.put("phone", String.valueOf(payload == null ? "13800009999" : payload.getOrDefault("phone", "13800009999")));
        item.put("status_code", String.valueOf(payload == null ? "ACTIVE" : payload.getOrDefault("status_code", "ACTIVE")));
        item.put("roles", new ArrayList<>());
        item.put("_password", password);
        item.put("last_login_at", now());
        adminUsers.add(0, item);
        return ApiResponse.ok(copyMap(item));
    }

    @GetMapping("/admin-users/{adminUserId}")
    public ApiResponse<Map<String, Object>> adminUserDetail(HttpServletRequest request, @PathVariable Long adminUserId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(adminUsers, adminUserId);
        if (target == null) return ApiResponse.fail(4041, "管理员不存在");
        return ApiResponse.ok(copyMap(target));
    }

    @PutMapping("/admin-users/{adminUserId}")
    public ApiResponse<Map<String, Object>> updateAdminUser(HttpServletRequest request,
                                                            @PathVariable Long adminUserId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(adminUsers, adminUserId);
        if (target == null) return ApiResponse.fail(4041, "管理员不存在");
        mergeFields(target, payload, "display_name", "phone", "status_code");
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/admin-users/{adminUserId}/status")
    public ApiResponse<Map<String, Object>> adminUserStatus(HttpServletRequest request,
                                                            @PathVariable Long adminUserId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(adminUsers, adminUserId);
        if (target == null) return ApiResponse.fail(4041, "管理员不存在");
        target.put("status_code", String.valueOf(payload == null ? "ACTIVE" : payload.getOrDefault("status_code", "ACTIVE")));
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/admin-users/{adminUserId}/roles")
    public ApiResponse<Map<String, Object>> adminUserRoles(HttpServletRequest request,
                                                           @PathVariable Long adminUserId,
                                                           @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(adminUsers, adminUserId);
        if (target == null) return ApiResponse.fail(4041, "管理员不存在");
        Object roleNames = payload == null ? null : payload.get("roles");
        if (roleNames instanceof Collection<?>) {
            target.put("roles", ((Collection<?>) roleNames).stream().map(String::valueOf).collect(Collectors.toList()));
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/admin-users/{adminUserId}/reset-password")
    public ApiResponse<Map<String, Object>> resetAdminUserPwd(HttpServletRequest request, @PathVariable Long adminUserId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(adminUsers, adminUserId);
        if (target == null) return ApiResponse.fail(4041, "管理员不存在");
        String tempPassword = String.valueOf(target.get("username")) + "@123";
        target.put("_password", tempPassword);
        return ApiResponse.ok(Map.of("temp_password", tempPassword));
    }

    @GetMapping("/roles")
    public ApiResponse<Map<String, Object>> listRoles(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String keyword) {
        requireAdminLike(request);
        List<Map<String, Object>> source = roles.stream()
                .filter(item -> !StringUtils.hasText(keyword) || matchKeyword(item, keyword, "role_name", "role_code", "remark"))
                .map(this::copyMap)
                .collect(Collectors.toList());
        return ApiResponse.ok(pageResult(source, pageNo, pageSize));
    }

    @PostMapping("/roles")
    public ApiResponse<Map<String, Object>> createRole(HttpServletRequest request,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        long id = roleIdSeq.incrementAndGet();
        item.put("id", id);
        item.put("role_name", String.valueOf(payload == null ? "新角色" : payload.getOrDefault("role_name", "新角色")));
        item.put("role_code", String.valueOf(payload == null ? "NEW_ROLE" : payload.getOrDefault("role_code", "NEW_ROLE")));
        item.put("status_code", String.valueOf(payload == null ? "ACTIVE" : payload.getOrDefault("status_code", "ACTIVE")));
        item.put("permission_count", 0);
        item.put("remark", String.valueOf(payload == null ? "" : payload.getOrDefault("remark", "")));
        roles.add(0, item);
        rolePermissionMap.put(id, new LinkedHashSet<>());
        return ApiResponse.ok(copyMap(item));
    }

    @GetMapping("/roles/{roleId}")
    public ApiResponse<Map<String, Object>> roleDetail(HttpServletRequest request, @PathVariable Long roleId) {
        requireAdminLike(request);
        Map<String, Object> target = findById(roles, roleId);
        if (target == null) return ApiResponse.fail(4041, "角色不存在");
        Map<String, Object> data = copyMap(target);
        List<Long> permissionIds = new ArrayList<>(rolePermissionMap.getOrDefault(roleId, new LinkedHashSet<>()));
        data.put("permission_ids", permissionIds);
        return ApiResponse.ok(data);
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<Map<String, Object>> updateRole(HttpServletRequest request,
                                                       @PathVariable Long roleId,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(roles, roleId);
        if (target == null) return ApiResponse.fail(4041, "角色不存在");
        mergeFields(target, payload, "role_name", "role_code", "status_code", "remark");
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<Map<String, Object>> deleteRole(HttpServletRequest request, @PathVariable Long roleId) {
        requireAdminLike(request);
        removeById(roles, roleId);
        rolePermissionMap.remove(roleId);
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public ApiResponse<Map<String, Object>> assignRolePermissions(HttpServletRequest request,
                                                                  @PathVariable Long roleId,
                                                                  @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> role = findById(roles, roleId);
        if (role == null) return ApiResponse.fail(4041, "角色不存在");

        Set<Long> ids = new LinkedHashSet<>();
        Object list = payload == null ? null : payload.get("permission_ids");
        if (list instanceof Collection<?>) {
            for (Object one : (Collection<?>) list) {
                ids.add(InMemoryData.toLong(one));
            }
        }
        rolePermissionMap.put(roleId, ids);
        role.put("permission_count", ids.size());
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/permissions")
    public ApiResponse<Map<String, Object>> listPermissions(HttpServletRequest request) {
        requireAdminLike(request);
        List<Map<String, Object>> source = permissions.stream().map(this::copyMap).collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", source));
    }

    @GetMapping("/permissions/tree")
    public ApiResponse<Map<String, Object>> permissionTree(HttpServletRequest request) {
        requireAdminLike(request);
        List<Map<String, Object>> tree = buildPermissionTree();
        return ApiResponse.ok(Map.of("list", tree));
    }

    @PostMapping("/permissions")
    public ApiResponse<Map<String, Object>> createPermission(HttpServletRequest request,
                                                             @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", permissionIdSeq.incrementAndGet());
        item.put("permission_name", String.valueOf(payload == null ? "新权限" : payload.getOrDefault("permission_name", "新权限")));
        item.put("permission_code", String.valueOf(payload == null ? "new:perm" : payload.getOrDefault("permission_code", "new:perm")));
        item.put("module_name", String.valueOf(payload == null ? "默认模块" : payload.getOrDefault("module_name", "默认模块")));
        item.put("method", String.valueOf(payload == null ? "GET" : payload.getOrDefault("method", "GET")));
        item.put("path", String.valueOf(payload == null ? "/api/v1/admin/new" : payload.getOrDefault("path", "/api/v1/admin/new")));
        Object parentId = payload == null ? null : payload.get("parent_id");
        item.put("parent_id", parentId == null ? null : InMemoryData.toLong(parentId));
        permissions.add(item);
        return ApiResponse.ok(copyMap(item));
    }

    @PutMapping("/permissions/{permissionId}")
    public ApiResponse<Map<String, Object>> updatePermission(HttpServletRequest request,
                                                             @PathVariable Long permissionId,
                                                             @RequestBody(required = false) Map<String, Object> payload) {
        requireAdminLike(request);
        Map<String, Object> target = findById(permissions, permissionId);
        if (target == null) return ApiResponse.fail(4041, "权限不存在");
        mergeFields(target, payload, "permission_name", "permission_code", "module_name", "method", "path", "parent_id");
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ApiResponse<Map<String, Object>> deletePermission(HttpServletRequest request, @PathVariable Long permissionId) {
        requireAdminLike(request);
        removeById(permissions, permissionId);
        for (Set<Long> set : rolePermissionMap.values()) {
            set.remove(permissionId);
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // fallback for remaining admin endpoints
    // =========================
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ApiResponse<Object> adminFallback(HttpServletRequest request,
                                             @RequestParam(required = false) Integer pageNo,
                                             @RequestParam(required = false) Integer pageSize) {
        requireAdminLike(request);

        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            if (pageNo != null || pageSize != null || request.getRequestURI().endsWith("s")) {
                return ApiResponse.ok(pageResult(new ArrayList<>(), pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize));
            }
            return ApiResponse.ok(new LinkedHashMap<>());
        }
        return ApiResponse.ok(Map.of("success", true));
    }

    // =========================
    // helper
    // =========================
    private void initMinimalSystemData() {
        if (adminUsers.isEmpty()) {
            adminUsers.add(adminUser(1L, "admin", "系统管理员", "13800001111", "ACTIVE",
                    List.of("超级管理员"), "admin123456", now()));
        }
        if (roles.isEmpty()) {
            roles.add(role(10L, "超级管理员", "SUPER_ADMIN", "ACTIVE", 0, "拥有全部权限"));
        }
        if (permissions.isEmpty()) {
            permissions.add(permission(101L, "仪表盘查看", "dashboard:view", "仪表盘", "GET", "/api/v1/admin/dashboard/overview", null));
        }
        if (!rolePermissionMap.containsKey(10L)) {
            rolePermissionMap.put(10L, permissions.stream()
                    .map(item -> InMemoryData.toLong(item.get("id")))
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
    }

    private boolean shouldBootstrapBusinessDataFromMysql() {
        if (mySqlStateStore == null || !mySqlStateStore.isReady()) return false;
        return spus.isEmpty() || categories.isEmpty();
    }

    private boolean bootstrapCoreDataFromBusinessTables() {
        if (mySqlStateStore == null || !mySqlStateStore.isReady()) return false;

        List<Map<String, Object>> bizCategories = mySqlStateStore.loadBizCategories();
        List<Map<String, Object>> bizSpus = mySqlStateStore.loadBizSpus();
        List<Map<String, Object>> bizSkus = mySqlStateStore.loadBizSkus();
        List<Map<String, Object>> bizMedias = mySqlStateStore.loadBizMedias();
        List<Map<String, Object>> bizUsers = mySqlStateStore.loadBizUsers();
        List<Map<String, Object>> bizAccounts = mySqlStateStore.loadBizPointAccounts();

        if (bizCategories.isEmpty() && bizSpus.isEmpty()) return false;

        Map<Long, String> categoryNameMap = new LinkedHashMap<>();
        Map<Long, Long> categoryProductCount = bizSpus.stream()
                .collect(Collectors.groupingBy(
                        item -> InMemoryData.toLong(item.get("category_id")),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        categories.clear();
        for (Map<String, Object> row : bizCategories) {
            Long id = InMemoryData.toLong(row.get("id"));
            if (id == null || id <= 0) continue;
            String name = String.valueOf(row.getOrDefault("category_name", ""));
            categoryNameMap.put(id, name);
            categories.add(category(
                    id,
                    name,
                    InMemoryData.toInt(row.getOrDefault("sort_no", 0)),
                    String.valueOf(row.getOrDefault("status_code", "ENABLED")),
                    categoryProductCount.getOrDefault(id, 0L).intValue(),
                    formatDateTime(row.get("updated_at"))
            ));
        }

        Map<Long, String> mainImageBySpu = new LinkedHashMap<>();
        bizMedias.stream()
                .filter(row -> "IMAGE".equalsIgnoreCase(String.valueOf(row.getOrDefault("media_type_code", ""))))
                .sorted((a, b) -> {
                    int cmp = Integer.compare(InMemoryData.toInt(b.getOrDefault("sort_no", 0)), InMemoryData.toInt(a.getOrDefault("sort_no", 0)));
                    if (cmp != 0) return cmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .forEach(row -> {
                    Long spuId = InMemoryData.toLong(row.get("spu_id"));
                    if (spuId == null || spuId <= 0 || mainImageBySpu.containsKey(spuId)) return;
                    mainImageBySpu.put(spuId, String.valueOf(row.getOrDefault("media_url", "")));
                });

        spus.clear();
        for (Map<String, Object> row : bizSpus) {
            Long id = InMemoryData.toLong(row.get("id"));
            if (id == null || id <= 0) continue;
            Long categoryId = InMemoryData.toLong(row.get("category_id"));
            String categoryName = categoryNameMap.getOrDefault(categoryId, "未分类");
            long pointPrice = InMemoryData.toLong(row.getOrDefault("point_price", 0));
            Map<String, Object> spu = spu(
                    id,
                    String.valueOf(row.getOrDefault("product_name", "")),
                    categoryName,
                    pointPrice,
                    pointPrice,
                    InMemoryData.toLong(row.getOrDefault("stock_available", 0)),
                    String.valueOf(row.getOrDefault("sale_status_code", "OFF_SHELF")),
                    InMemoryData.toInt(row.getOrDefault("recommend_flag", 0)) == 1,
                    formatDateTime(row.get("updated_at"))
            );
            spu.put("category_id", categoryId);
            spu.put("detail_html", row.get("detail_html") == null ? "" : String.valueOf(row.get("detail_html")));
            spus.add(spu);

            if (appStore != null) {
                appStore.upsertProductFromAdmin(
                        id,
                        categoryId,
                        String.valueOf(row.getOrDefault("product_name", "")),
                        String.valueOf(row.getOrDefault("product_type_code", "PHYSICAL")),
                        pointPrice,
                        InMemoryData.toLong(row.getOrDefault("stock_available", 0)),
                        String.valueOf(row.getOrDefault("sale_status_code", "OFF_SHELF")),
                        StringUtils.hasText(String.valueOf(row.getOrDefault("main_image_url", "")))
                                ? String.valueOf(row.getOrDefault("main_image_url", ""))
                                : mainImageBySpu.getOrDefault(id, ""),
                        row.get("detail_html") == null ? "" : String.valueOf(row.get("detail_html")),
                        InMemoryData.toInt(row.getOrDefault("limit_per_user", 0))
                );
            }
        }

        skus.clear();
        for (Map<String, Object> row : bizSkus) {
            Long id = InMemoryData.toLong(row.get("id"));
            Long spuId = InMemoryData.toLong(row.get("spu_id"));
            if (id == null || id <= 0 || spuId == null || spuId <= 0) continue;
            String saleStatus = String.valueOf(row.getOrDefault("sale_status_code", "OFF_SHELF"));
            skus.add(sku(
                    id,
                    spuId,
                    String.valueOf(row.getOrDefault("sku_name", "")),
                    String.valueOf(row.getOrDefault("sku_name", "")),
                    InMemoryData.toLong(row.getOrDefault("point_price", 0)),
                    InMemoryData.toLong(row.getOrDefault("stock_available", 0)),
                    "ON_SHELF".equalsIgnoreCase(saleStatus) ? "ENABLED" : "DISABLED"
            ));
        }

        medias.clear();
        for (Map<String, Object> row : bizMedias) {
            Long id = InMemoryData.toLong(row.get("id"));
            Long spuId = InMemoryData.toLong(row.get("spu_id"));
            if (id == null || id <= 0 || spuId == null || spuId <= 0) continue;
            medias.add(media(
                    id,
                    spuId,
                    String.valueOf(row.getOrDefault("media_type_code", "IMAGE")),
                    String.valueOf(row.getOrDefault("media_url", "")),
                    InMemoryData.toInt(row.getOrDefault("sort_no", 0))
            ));
        }

        Map<Long, Map<String, Object>> pointByUserId = new LinkedHashMap<>();
        for (Map<String, Object> acc : bizAccounts) {
            Long userId = InMemoryData.toLong(acc.get("user_id"));
            if (userId != null && userId > 0) {
                pointByUserId.put(userId, acc);
            }
        }

        users.clear();
        for (Map<String, Object> row : bizUsers) {
            Long id = InMemoryData.toLong(row.get("id"));
            if (id == null || id <= 0) continue;
            Map<String, Object> acc = pointByUserId.get(id);
            long balance = acc == null ? 0L : InMemoryData.toLong(acc.getOrDefault("point_balance", 0));
            Map<String, Object> extJson = parseExtJsonMap(row.get("ext_json"));
            String phoneRaw = String.valueOf(row.getOrDefault("phone", "")).trim();
            String phoneMasked = String.valueOf(row.getOrDefault("phone_masked", "")).trim();
            String openId = String.valueOf(row.getOrDefault("open_id", "")).trim();
            String unionId = String.valueOf(row.getOrDefault("union_id", "")).trim();
            String avatarUrl = String.valueOf(row.getOrDefault("avatar_url", "")).trim();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("nick_name", String.valueOf(row.getOrDefault("nick_name", "碎片用户")));
            item.put("phone", phoneRaw);
            item.put("phone_masked", phoneMasked);
            item.put("open_id", openId);
            item.put("union_id", unionId);
            item.put("avatar_url", avatarUrl);
            item.put("user_status_code", mapUserStatus(String.valueOf(row.getOrDefault("status_code", "ACTIVE"))));
            item.put("point_balance", balance);
            item.put("order_count", 0);
            item.put("admin_remark", String.valueOf(row.getOrDefault("remark", "")));
            item.put("total_consume_amount", toMoney(extJson.getOrDefault("total_consume_amount", 0D)));
            item.put("profit_amount", toMoney(extJson.getOrDefault("profit_amount", 0D)));
            item.put("created_at", formatDateTime(row.get("created_at")));
            users.add(item);

            if (appStore != null) {
                appStore.upsertUserPointFromAdmin(
                        id,
                        String.valueOf(row.getOrDefault("nick_name", "碎片用户")),
                        phoneMasked,
                        mapUserStatusToApp(String.valueOf(row.getOrDefault("status_code", "ACTIVE"))),
                        balance,
                        toMoney(row.getOrDefault("total_consume_amount", 0D)),
                        toMoney(row.getOrDefault("profit_amount", 0D))
                );
                appStore.syncWechatIdentityFromAdmin(
                        id,
                        openId,
                        unionId,
                        phoneRaw,
                        phoneMasked,
                        avatarUrl
                );
            }
        }

        syncIdSequencesByData();
        return true;
    }

    private void initSeed() {
        users.add(user(1001L, "积分用户A", "13812345678", "ACTIVE", 16888, 6, "2026-03-01 11:20:00"));
        users.add(user(1002L, "潮玩用户B", "13922348899", "ACTIVE", 3200, 2, "2026-03-06 09:40:00"));
        users.add(user(1003L, "薅羊毛用户C", "13700002233", "FROZEN", 120, 1, "2026-03-08 16:30:00"));
        users.add(user(1004L, "茅台爱好者", "18699887755", "ACTIVE", 4280, 4, "2026-03-10 10:08:00"));

        userAddresses.add(address(20001L, 1001L, "张三", "13800001111", "上海市浦东新区世纪大道100号"));
        userAddresses.add(address(20002L, 1002L, "李四", "13900002222", "北京市朝阳区建国路88号"));

        pointLedger.add(ledger(50001L, 1001L, "积分用户A", "EXCHANGE_ORDER", -1888, 16888, "2026-03-17 09:18:00"));
        pointLedger.add(ledger(50002L, 1002L, "潮玩用户B", "MANUAL_ADJUST", 500, 3200, "2026-03-17 10:05:00"));
        pointLedger.add(ledger(50003L, 1004L, "茅台爱好者", "ORDER_CANCEL_REFUND", 3300, 4280, "2026-03-16 13:25:00"));

        categories.add(category(11L, "白酒", 100, "ENABLED", 18, "2026-03-17 10:10:00"));
        categories.add(category(12L, "数码", 90, "ENABLED", 24, "2026-03-17 10:09:00"));
        categories.add(category(13L, "生活权益", 80, "ENABLED", 36, "2026-03-17 10:08:00"));
        categories.add(category(14L, "潮流周边", 70, "DISABLED", 6, "2026-03-16 20:40:00"));

        recommendSlots.add(slot(201L, "首页推荐", "HOME_RECOMMEND", "ENABLED", "2026-03-17 11:20:00"));
        recommendSlots.add(slot(202L, "推荐上新", "HOME_NEW_ARRIVAL", "ENABLED", "2026-03-17 11:15:00"));
        recommendSlots.add(slot(203L, "猜你喜欢", "HOME_GUESS", "DISABLED", "2026-03-15 09:15:00"));

        recommendItems.add(recommendItem(9001L, 201L, 101L, "飞天茅台53度 500ML×1", 1888, 100, "ENABLED", "2026-03-01 00:00:00", "2026-12-31 23:59:59"));
        recommendItems.add(recommendItem(9002L, 201L, 102L, "苹果 Watch Ultra3", 5988, 90, "ENABLED", "2026-03-01 00:00:00", "2026-12-31 23:59:59"));
        recommendItems.add(recommendItem(9004L, 202L, 104L, "每天发红包牛票", 888, 100, "ENABLED", "2026-03-10 00:00:00", "2026-12-31 23:59:59"));

        spus.add(spu(101L, "飞天茅台53度 500ML", "白酒", 1888, 2388, 106, "ON_SHELF", true, "2026-03-17 15:12:00"));
        spus.add(spu(102L, "苹果 Watch Ultra3", "数码", 5988, 6988, 47, "ON_SHELF", true, "2026-03-17 15:09:00"));
        spus.add(spu(103L, "至臻玩家微信群入群资格", "生活权益", 8888, 8888, 9999, "OFF_SHELF", false, "2026-03-16 20:30:00"));

        skus.add(sku(5001L, 101L, "标准装", "500ml x 1", 1888, 58, "ENABLED"));
        skus.add(sku(5002L, 101L, "礼盒装", "500ml x 1 + 礼袋", 2388, 48, "ENABLED"));
        skus.add(sku(5003L, 102L, "49mm 钛金属", "黑色海洋表带", 5988, 27, "ENABLED"));
        skus.add(sku(5004L, 102L, "49mm 钛金属", "橙色高山回环表带", 6988, 20, "ENABLED"));
        skus.add(sku(5005L, 103L, "入群名额", "有效期一年", 8888, 9999, "DISABLED"));

        medias.add(media(8001L, 101L, "IMAGE", "https://picsum.photos/id/1060/800/500", 100));
        medias.add(media(8002L, 101L, "IMAGE", "https://picsum.photos/id/292/800/500", 90));
        medias.add(media(8003L, 102L, "IMAGE", "https://picsum.photos/id/3/800/500", 100));

        attrDefs.add(attrDef(9001L, "容量", "CAPACITY", "TEXT", true, "ENABLED", "2026-03-17 11:10:00"));
        attrDefs.add(attrDef(9002L, "酒精度", "ABV", "NUMBER", true, "ENABLED", "2026-03-17 11:09:00"));
        attrDefs.add(attrDef(9003L, "版本", "EDITION", "ENUM", false, "ENABLED", "2026-03-17 11:08:00"));

        orders.add(order(30001L, 1001L, "EO100001", "积分用户A", "PENDING_AUDIT", 1888, "2026-03-17 09:18:00", ""));
        orders.add(order(30002L, 1002L, "EO100002", "潮玩用户B", "PENDING_SHIP", 5988, "2026-03-17 10:23:00", "优先发货"));
        orders.add(order(30003L, 1001L, "EO100003", "积分用户A", "SHIPPED", 888, "2026-03-16 21:42:00", ""));
        orders.add(order(30004L, 1004L, "EO100004", "茅台爱好者", "FINISHED", 3300, "2026-03-14 12:11:00", "VIP用户"));

        orderItems.add(orderItem(1L, 30001L, "飞天茅台53度 500ML", "标准装", 1, 1888));
        orderItems.add(orderItem(2L, 30002L, "苹果 Watch Ultra3", "49mm 钛金属 黑色表带", 1, 5988));
        orderItems.add(orderItem(3L, 30003L, "每天发红包牛票", "权益券", 1, 888));
        orderItems.add(orderItem(4L, 30004L, "茅台酒厂纪念礼盒", "礼盒版", 1, 3300));

        orderFlows.add(orderFlow(1001L, 30001L, "INIT", "PENDING_AUDIT", "用户提交订单", "", "系统", "2026-03-17 09:18:00"));
        orderFlows.add(orderFlow(1002L, 30002L, "INIT", "PENDING_AUDIT", "用户提交订单", "", "系统", "2026-03-17 10:23:00"));
        orderFlows.add(orderFlow(1003L, 30002L, "PENDING_AUDIT", "PENDING_SHIP", "审核通过", "库存充足", "系统管理员", "2026-03-17 10:35:00"));
        orderFlows.add(orderFlow(1004L, 30003L, "PENDING_SHIP", "SHIPPED", "发货", "顺丰 SF000003", "仓库管理员", "2026-03-17 08:00:00"));
        orderFlows.add(orderFlow(1005L, 30004L, "SHIPPED", "FINISHED", "完成", "用户已签收", "系统管理员", "2026-03-16 15:20:00"));

        orderDeliveries.add(delivery(30003L, "张三", "13800001111", "上海市浦东新区世纪大道100号", "顺丰", "SF000003", "2026-03-17 08:00:00"));
        orderDeliveries.add(delivery(30004L, "李四", "13900002222", "北京市朝阳区建国路88号", "京东", "JD000004", "2026-03-14 14:00:00"));

        backpackAssets.add(asset(7001L, "BA202603170001", "积分用户A", "至臻玩家微信群入群资格", "GROUP_QR", "ACTIVE", "2026-03-17 09:20:00", "2026-12-31 23:59:59"));
        backpackAssets.add(asset(7002L, "BA202603160002", "潮玩用户B", "潮玩盲盒兑换券", "COUPON", "USED", "2026-03-16 10:30:00", "2026-10-01 00:00:00"));
        backpackAssets.add(asset(7003L, "BA202603150003", "茅台爱好者", "飞天茅台优先购买资格", "PHYSICAL", "EXPIRED", "2026-03-15 08:00:00", "2026-03-16 23:59:59"));

        backpackFlows.add(assetFlow(90001L, 7001L, "GRANT", "发放资产", "运营活动赠送", "系统管理员", "2026-03-17 09:20:00"));
        backpackFlows.add(assetFlow(90002L, 7002L, "GRANT", "发放资产", "积分兑换", "系统", "2026-03-16 10:30:00"));
        backpackFlows.add(assetFlow(90003L, 7002L, "USE", "核销使用", "用户兑换成功", "系统", "2026-03-16 12:20:00"));
        backpackFlows.add(assetFlow(90005L, 7003L, "EXPIRE", "资产过期", "系统自动过期", "系统", "2026-03-17 00:00:00"));

        groupResources.add(group(8101L, "茅台玩家交流群", "https://picsum.photos/id/180/300/300", "每日行情、酒友交流、官方活动第一时间通知", 500, 289, "2026-12-31 23:59:59", "ENABLED", "2026-03-17 14:00:00"));
        groupResources.add(group(8102L, "潮玩福利群", "https://picsum.photos/id/201/300/300", "每周掉落新品兑换福利", 300, 198, "2026-09-30 23:59:59", "ENABLED", "2026-03-17 13:00:00"));
        groupResources.add(group(8103L, "高阶会员内测群", "https://picsum.photos/id/433/300/300", "功能内测与意见反馈", 200, 56, "2026-06-30 23:59:59", "DISABLED", "2026-03-16 16:00:00"));

        dictTypes.add(dictType(1001L, "ORDER_STATUS", "订单状态", "ENABLED", "订单状态字典", "2026-03-17 10:10:00"));
        dictTypes.add(dictType(1002L, "ASSET_STATUS", "资产状态", "ENABLED", "背包资产状态", "2026-03-17 10:09:00"));
        dictTypes.add(dictType(1003L, "USER_LEVEL", "用户等级", "DISABLED", "会员等级字典", "2026-03-16 19:30:00"));

        dictItems.add(dictItem(2001L, "ORDER_STATUS", "PENDING_AUDIT", "待审核", "PENDING_AUDIT", 100, "ENABLED", "2026-03-17 10:00:00"));
        dictItems.add(dictItem(2002L, "ORDER_STATUS", "PENDING_SHIP", "待发货", "PENDING_SHIP", 90, "ENABLED", "2026-03-17 10:00:00"));
        dictItems.add(dictItem(2101L, "ASSET_STATUS", "ACTIVE", "有效", "ACTIVE", 100, "ENABLED", "2026-03-17 09:58:00"));

        systemConfigs.add(config(3001L, "mall.home.banner_autoplay", "首页轮播自动播放", "true", "BOOLEAN", "MALL_HOME", "ENABLED", "控制首页 banner 自动轮播", "2026-03-17 12:00:00"));
        systemConfigs.add(config(3002L, "mall.exchange.default_limit", "默认兑换限购", "1", "NUMBER", "MALL_EXCHANGE", "ENABLED", "未单独配置时的默认限购数", "2026-03-17 11:58:00"));
        systemConfigs.add(config(3003L, "mall.customer_service.contact", "客服联系方式", "抖店客服：18888888888", "STRING", "MALL_SERVICE", "DISABLED", "前台客服联系文案", "2026-03-16 14:22:00"));
        systemConfigs.add(config(3004L, "mall.profile.group_entry_enabled", "个人中心群聊入口开关", "false", "BOOLEAN", "MALL_PROFILE", "ENABLED", "关闭时小程序不显示加入群聊入口", "2026-03-20 09:00:00"));
        systemConfigs.add(config(3005L, "mall.profile.group_entry_text", "个人中心群聊入口文案", "加入群聊", "STRING", "MALL_PROFILE", "ENABLED", "小程序个人中心群聊入口显示文案", "2026-03-20 09:00:00"));
        systemConfigs.add(config(3006L, "mall.profile.group_entry_qrcode_url", "个人中心群聊二维码地址", "", "STRING", "MALL_PROFILE", "ENABLED", "点击加入群聊后弹窗展示的二维码图片地址", "2026-03-23 10:00:00"));
        systemConfigs.add(config(3007L, "mall.profile.group_entry_desc_enabled", "个人中心群聊说明文案开关", "false", "BOOLEAN", "MALL_PROFILE", "ENABLED", "控制是否显示加入群聊下方说明文案", "2026-03-23 10:00:00"));
        systemConfigs.add(config(3008L, "mall.profile.group_entry_desc_text", "个人中心群聊说明文案", "", "STRING", "MALL_PROFILE", "ENABLED", "显示在加入群聊下方的小字说明", "2026-03-23 10:00:00"));

        wishDemands.add(wishDemand(10001L, 1001L, "积分用户A", "138****5678", "希望上架：Switch 游戏卡带", "想要马里奥系列，最好有国行版", "", "PENDING", "", "", ""));

        files.add(file(4001L, "banner-home-01.png", "https://picsum.photos/id/20/960/420", "image/png", 512, "2026-03-17 08:30:00"));
        files.add(file(4002L, "group-qr-01.jpg", "https://picsum.photos/id/37/300/300", "image/jpeg", 182, "2026-03-17 09:12:00"));

        adminUsers.add(adminUser(1L, "admin", "系统管理员", "13800001111", "ACTIVE", List.of("超级管理员"), "admin123456", "2026-03-17 14:30:00"));
        adminUsers.add(adminUser(2L, "ops_lead", "运营主管", "13800002222", "ACTIVE", List.of("订单运营", "商品运营"), "ops_lead@123", "2026-03-17 10:08:00"));
        adminUsers.add(adminUser(3L, "auditor", "审核专员", "13800003333", "FROZEN", List.of("订单审核"), "auditor@123", "2026-03-16 18:21:00"));

        permissions.add(permission(101L, "仪表盘查看", "dashboard:view", "仪表盘", "GET", "/api/v1/admin/dashboard/overview", null));
        permissions.add(permission(201L, "用户列表查看", "users:view", "用户管理", "GET", "/api/v1/admin/users", null));
        permissions.add(permission(202L, "用户冻结/解冻", "users:status", "用户管理", "PUT", "/api/v1/admin/users/{userId}/status", 201L));
        permissions.add(permission(203L, "积分调整", "users:points_adjust", "用户管理", "POST", "/api/v1/admin/users/{userId}/points/adjust", 201L));
        permissions.add(permission(204L, "用户财务编辑", "users:finance", "用户管理", "PUT", "/api/v1/admin/users/{userId}/finance", 201L));
        permissions.add(permission(301L, "商品列表查看", "products:view", "商品中心", "GET", "/api/v1/admin/products/spu", null));
        permissions.add(permission(302L, "商品新增", "products:create", "商品中心", "POST", "/api/v1/admin/products/spu", 301L));
        permissions.add(permission(401L, "订单列表查看", "orders:view", "订单中心", "GET", "/api/v1/admin/orders", null));
        permissions.add(permission(402L, "订单审核通过", "orders:approve", "订单中心", "POST", "/api/v1/admin/orders/{orderId}/approve", 401L));
        permissions.add(permission(403L, "订单发货", "orders:ship", "订单中心", "POST", "/api/v1/admin/orders/{orderId}/ship", 401L));
        permissions.add(permission(501L, "角色列表查看", "roles:view", "权限管理", "GET", "/api/v1/admin/roles", null));
        permissions.add(permission(502L, "角色新增", "roles:create", "权限管理", "POST", "/api/v1/admin/roles", 501L));
        permissions.add(permission(503L, "角色授权", "roles:grant", "权限管理", "PUT", "/api/v1/admin/roles/{roleId}/permissions", 501L));

        roles.add(role(10L, "超级管理员", "SUPER_ADMIN", "ACTIVE", 12, "拥有全部权限"));
        roles.add(role(11L, "订单审核", "ORDER_AUDIT", "ACTIVE", 7, "处理审核与发货流程"));
        roles.add(role(12L, "商品运营", "PRODUCT_OPS", "ACTIVE", 6, "维护商品和推荐位"));

        rolePermissionMap.put(10L, permissions.stream().map(item -> InMemoryData.toLong(item.get("id"))).collect(Collectors.toCollection(LinkedHashSet::new)));
        rolePermissionMap.put(11L, new LinkedHashSet<>(Arrays.asList(401L, 402L, 403L, 201L, 202L, 203L, 204L, 101L)));
        rolePermissionMap.put(12L, new LinkedHashSet<>(Arrays.asList(301L, 302L, 101L, 501L, 502L, 503L)));

        pointRules.add(pointRule(1L, "注册赠送积分", "REGISTER_GIFT", 100, "ENABLED", "新用户注册奖励"));
        pointRules.add(pointRule(2L, "每日签到", "DAILY_SIGN", 5, "ENABLED", "每日签到积分"));
    }

    private void syncFromMiniProgramCoreData() {
        if (appStore == null) return;
        normalizeDuplicateUsersFromMiniProgramStore();
        Set<Long> appUserIds = appStore.listUsers().stream()
                .map(item -> InMemoryData.toLong(item.get("id")))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (appUserIds.isEmpty()) {
            appUserIds.add(1001L);
        }
        for (Long appUserId : appUserIds) {
            syncUserFromMiniProgram(appUserId);
            syncUserAddressesFromMiniProgram(appUserId);
            syncOrdersFromMiniProgram(appUserId);
            syncPointLedgerFromMiniProgram(appUserId);
            syncAssetsFromMiniProgram(appUserId);
        }
        syncCategoriesFromMiniProgram();
        syncProductsFromMiniProgram();
        syncGroupResourcesFromMiniProgram();
    }

    private void syncUserAddressesFromMiniProgram(Long userId) {
        List<Map<String, Object>> appAddresses = new ArrayList<>(appStore.userAddressMap(userId).values());
        userAddresses.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("user_id")), userId));
        for (Map<String, Object> appAddress : appAddresses) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("id", InMemoryData.toLong(appAddress.getOrDefault("id", 0)));
            mapped.put("user_id", userId);
            mapped.put("receiver_name", String.valueOf(appAddress.getOrDefault("receiver_name", "")));
            mapped.put("receiver_phone", String.valueOf(appAddress.getOrDefault("receiver_phone", "")));
            mapped.put("country_code", String.valueOf(appAddress.getOrDefault("country_code", "CN")));
            mapped.put("province_name", String.valueOf(appAddress.getOrDefault("province_name", "")));
            mapped.put("city_name", String.valueOf(appAddress.getOrDefault("city_name", "")));
            mapped.put("district_name", String.valueOf(appAddress.getOrDefault("district_name", "")));
            mapped.put("detail_address", String.valueOf(appAddress.getOrDefault("detail_address", "")));
            mapped.put("is_default", InMemoryData.toInt(appAddress.getOrDefault("is_default", 0)));
            mapped.put("status_code", String.valueOf(appAddress.getOrDefault("status_code", "ACTIVE")));
            mapped.put("created_at", String.valueOf(appAddress.getOrDefault("created_at", now())));
            userAddresses.add(mapped);
        }
    }

    private void syncUserFromMiniProgram(Long userId) {
        Map<String, Object> appUser = appStore.getUser(userId);
        Map<String, Object> pointAccount = appStore.getPointAccount(userId);
        if (appUser == null || pointAccount == null) return;

        Map<String, Object> target = findById(users, userId);
        if (target == null) {
            target = new LinkedHashMap<>();
            target.put("id", userId);
            target.put("created_at", String.valueOf(appUser.getOrDefault("created_at", now())));
            users.add(target);
        }
        String phoneMasked = String.valueOf(appUser.getOrDefault("phone_masked", ""));
        String nickName = String.valueOf(appUser.getOrDefault("nick_name", "积分用户"));
        if (WECHAT_DEFAULT_NICK_PATTERN.matcher(nickName.trim()).matches()) {
            String digits = phoneMasked == null ? "" : phoneMasked.replaceAll("\\D", "");
            if (StringUtils.hasText(digits) && digits.length() >= 4) {
                nickName = "用户" + digits.substring(digits.length() - 4);
            } else {
                nickName = "用户" + userId;
            }
        }
        target.put("nick_name", nickName);
        target.put("phone_masked", phoneMasked);
        String phoneRaw = String.valueOf(appUser.getOrDefault("phone", "")).trim();
        if (StringUtils.hasText(phoneRaw)) {
            target.put("phone", phoneRaw);
        }
        String openId = String.valueOf(appUser.getOrDefault("open_id", "")).trim();
        if (StringUtils.hasText(openId)) {
            target.put("open_id", openId);
        }
        String unionId = String.valueOf(appUser.getOrDefault("union_id", "")).trim();
        if (StringUtils.hasText(unionId)) {
            target.put("union_id", unionId);
        }
        String avatarUrl = String.valueOf(appUser.getOrDefault("avatar_url", "")).trim();
        if (StringUtils.hasText(avatarUrl)) {
            target.put("avatar_url", avatarUrl);
        }
        target.put("user_status_code", mapUserStatus(String.valueOf(appUser.getOrDefault("user_status_code", "ACTIVE"))));
        target.put("point_balance", InMemoryData.toLong(pointAccount.getOrDefault("point_balance", 0)));
        target.put("order_count", appStore.countOrders(userId));
        if (!target.containsKey("admin_remark")) {
            target.put("admin_remark", "");
        }
        ensureUserExtraFields(target);
    }

    private void syncCategoriesFromMiniProgram() {
        for (Map<String, Object> item : appStore.listCategories()) {
            Long id = InMemoryData.toLong(item.get("id"));
            Map<String, Object> target = findById(categories, id);
            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("id", id);
                categories.add(target);
            }
            target.put("category_name", String.valueOf(item.getOrDefault("category_name", "")));
            target.put("sort_no", InMemoryData.toInt(item.getOrDefault("sort_no", 0)));
            target.put("status_code", mapCategoryStatus(String.valueOf(item.getOrDefault("status_code", "ACTIVE"))));
            long count = appStore.listProducts().stream()
                    .filter(p -> matchesCategoryIdFromProduct(p, id))
                    .count();
            target.put("product_count", count);
            target.put("updated_at", now());
        }
    }

    private void syncProductsFromMiniProgram() {
        Map<Long, String> categoryNames = appStore.listCategories().stream()
                .collect(Collectors.toMap(
                        item -> InMemoryData.toLong(item.get("id")),
                        item -> String.valueOf(item.getOrDefault("category_name", "未分类")),
                        (a, b) -> a
                ));

        for (Map<String, Object> item : appStore.listProducts()) {
            Long id = InMemoryData.toLong(item.get("id"));
            Map<String, Object> target = findById(spus, id);
            boolean recommendFlag = target != null && Boolean.TRUE.equals(target.get("recommend_flag"));
            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("id", id);
                spus.add(target);
            }
            target.put("spu_name", String.valueOf(item.getOrDefault("product_name", "")));
            Long appCategoryId = InMemoryData.toLong(item.get("category_id"));
            List<Long> appCategoryIds = extractCategoryIdsFromProduct(item);
            if (appCategoryIds.isEmpty() && appCategoryId != null && appCategoryId > 0) {
                appCategoryIds.add(appCategoryId);
            }
            if (!appCategoryIds.isEmpty()) {
                target.put("category_id", appCategoryIds.get(0));
                target.put("category_ids", new ArrayList<>(appCategoryIds));
            } else {
                target.put("category_id", appCategoryId);
            }
            List<String> resolvedCategoryNames = appCategoryIds.stream()
                    .map(categoryNames::get)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (resolvedCategoryNames.isEmpty() && StringUtils.hasText(categoryNames.get(appCategoryId))) {
                resolvedCategoryNames.add(categoryNames.get(appCategoryId));
            }
            if (resolvedCategoryNames.isEmpty()) {
                String fallback = String.valueOf(target.getOrDefault("category_name", "")).trim();
                if (StringUtils.hasText(fallback)) {
                    resolvedCategoryNames.addAll(resolveCategoryNames(Map.of("category_name", fallback), fallback));
                }
            }
            if (resolvedCategoryNames.isEmpty()) {
                resolvedCategoryNames.add("未分类");
            }
            target.put("category_name", String.join(" / ", resolvedCategoryNames));
            target.put("category_names", new ArrayList<>(resolvedCategoryNames));
            long pointPrice = InMemoryData.toLong(item.getOrDefault("point_price", 0));
            target.put("point_price_min", pointPrice);
            target.put("point_price_max", pointPrice);
            target.put("total_stock", InMemoryData.toLong(item.getOrDefault("stock_available", 0)));
            target.put("status_code", String.valueOf(item.getOrDefault("status_code", "OFF_SHELF")));
            target.put("recommend_flag", recommendFlag);
            target.put("updated_at", now());
        }
    }

    private void syncOrdersFromMiniProgram(Long userId) {
        Map<String, Object> appUser = appStore.getUser(userId);
        String userName = appUser == null ? "积分用户" : String.valueOf(appUser.getOrDefault("nick_name", "积分用户"));

        List<Map<String, Object>> appOrders = appStore.listOrdersByUser(userId, null);
        for (Map<String, Object> appOrder : appOrders) {
            Long orderId = InMemoryData.toLong(appOrder.get("id"));
            Map<String, Object> target = findById(orders, orderId);
            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("id", orderId);
                orders.add(target);
            }
            boolean shouldOverwrite = shouldOverwriteExistingOrderFromApp(target, appOrder, userId);
            if (!shouldOverwrite) {
                continue;
            }
            // 执行upsert，避免历史测试数据ID冲突导致管理端看不到新订单
            target.put("user_id", userId);
            target.put("order_no", String.valueOf(appOrder.getOrDefault("order_no", "")));
            target.put("user_name", userName);
            target.put("order_status_code", mapOrderStatus(String.valueOf(appOrder.getOrDefault("order_status_code", "PENDING_AUDIT"))));
            target.put("total_point_amount", InMemoryData.toLong(appOrder.getOrDefault("total_point_amount", 0)));
            target.put("submit_at", String.valueOf(appOrder.getOrDefault("submit_at", appOrder.getOrDefault("created_at", now()))));
            String rejectReason = String.valueOf(appOrder.getOrDefault("reject_reason", ""));
            String appAdminRemark = String.valueOf(appOrder.getOrDefault("admin_remark", ""));
            String appUserRemark = String.valueOf(appOrder.getOrDefault("user_remark", ""));
            String finalRemark = StringUtils.hasText(rejectReason)
                    ? rejectReason
                    : (StringUtils.hasText(appAdminRemark) ? appAdminRemark : appUserRemark);
            target.put("remark", finalRemark);
            target.put("user_remark", appUserRemark);
            target.put("reject_reason", rejectReason);
            target.put("buyer_decision_required", Boolean.parseBoolean(String.valueOf(appOrder.getOrDefault("buyer_decision_required", false))));
            target.put("point_refunded", Boolean.parseBoolean(String.valueOf(appOrder.getOrDefault("point_refunded", false))));
            target.put("main_image_snapshot", String.valueOf(appOrder.getOrDefault("main_image_snapshot", appOrder.getOrDefault("main_image_url", ""))));
            target.put("updated_at", now());
            target.put("_from_app", true);
            target.put("procurement_status", String.valueOf(appOrder.getOrDefault("procurement_status", target.getOrDefault("procurement_status", "PENDING_PROCURE"))));
            target.put("procured_at", String.valueOf(appOrder.getOrDefault("procured_at", target.getOrDefault("procured_at", ""))));
            target.put("procured_by", String.valueOf(appOrder.getOrDefault("procured_by", target.getOrDefault("procured_by", ""))));
            ensureOrderProcurementFields(target);

            orderItems.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId));
            List<Map<String, Object>> appItems = appStore.getOrderItems(orderId);
            Set<String> dedupKeys = new HashSet<>();
            for (Map<String, Object> appItem : appItems) {
                Map<String, Object> mapped = new LinkedHashMap<>();
                mapped.put("id", InMemoryData.toLong(appItem.getOrDefault("id", 0)));
                mapped.put("order_id", orderId);
                mapped.put("spu_id", InMemoryData.toLong(appItem.getOrDefault("spu_id", appItem.getOrDefault("product_id", 0))));
                mapped.put("spu_name", String.valueOf(appItem.getOrDefault("product_name_snapshot", "")));
                mapped.put("sku_name", String.valueOf(appItem.getOrDefault("sku_name_snapshot", appItem.getOrDefault("product_name_snapshot", ""))));
                mapped.put("main_image_snapshot", String.valueOf(appItem.getOrDefault("main_image_snapshot", appItem.getOrDefault("main_image_url", ""))));
                mapped.put("quantity", InMemoryData.toInt(appItem.getOrDefault("quantity", 1)));
                mapped.put("point_price", InMemoryData.toLong(appItem.getOrDefault("unit_point_price", 0)));
                mapped.put("total_point_amount", InMemoryData.toLong(appItem.getOrDefault("total_point_amount", 0)));
                String dedupKey = buildOrderItemDedupKey(mapped);
                if (!dedupKeys.add(dedupKey)) {
                    continue;
                }
                orderItems.add(mapped);
            }

            orderFlows.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId));
            List<Map<String, Object>> appFlows = appStore.getOrderFlows(orderId);
            for (Map<String, Object> appFlow : appFlows) {
                Map<String, Object> mapped = new LinkedHashMap<>();
                mapped.put("id", InMemoryData.toLong(appFlow.getOrDefault("id", 0)));
                mapped.put("order_id", orderId);
                mapped.put("from_status", mapOrderStatus(String.valueOf(appFlow.getOrDefault("from_status_code", "INIT"))));
                mapped.put("to_status", mapOrderStatus(String.valueOf(appFlow.getOrDefault("to_status_code", "PENDING_AUDIT"))));
                mapped.put("action_text", mapFlowAction(String.valueOf(appFlow.getOrDefault("action_code", ""))));
                mapped.put("note", String.valueOf(appFlow.getOrDefault("remark", "")));
                mapped.put("operator_name", "系统");
                mapped.put("occurred_at", String.valueOf(appFlow.getOrDefault("operated_at", now())));
                orderFlows.add(mapped);
            }

            Map<String, Object> appDelivery = appStore.getOrderDelivery(orderId);
            Map<String, Object> appAddress = appStore.getOrderAddressSnapshot(orderId);
            orderDeliveries.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId));
            Map<String, Object> mappedDelivery = new LinkedHashMap<>();
            mappedDelivery.put("order_id", orderId);
            mappedDelivery.put("receiver_name", appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("receiver_name", "")));
            mappedDelivery.put("receiver_phone", appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("receiver_phone", "")));
            mappedDelivery.put("province_name", appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("province_name", "")));
            mappedDelivery.put("city_name", appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("city_name", "")));
            mappedDelivery.put("district_name", appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("district_name", "")));
            mappedDelivery.put("receiver_address", appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("detail_address", "")));
            mappedDelivery.put("express_company", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("company_name", "")));
            mappedDelivery.put("express_no", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("tracking_no", "")));
            mappedDelivery.put("shipper_code", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("shipper_code", "")));
            mappedDelivery.put("delivery_status_code", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("delivery_status_code", "")));
            mappedDelivery.put("delivery_status_text", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("delivery_status_text", "")));
            mappedDelivery.put("ship_at", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("ship_at", "")));
            mappedDelivery.put("signed_at", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("signed_at", "")));
            mappedDelivery.put("latest_trace_time", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("latest_trace_time", "")));
            mappedDelivery.put("latest_trace_station", appDelivery == null ? "" : String.valueOf(appDelivery.getOrDefault("latest_trace_station", "")));
            orderDeliveries.add(mappedDelivery);
        }
    }

    private boolean shouldOverwriteExistingOrderFromApp(Map<String, Object> target,
                                                        Map<String, Object> appOrder,
                                                        Long appUserId) {
        if (target == null) return true;
        if (Boolean.TRUE.equals(target.get("_order_admin_touched"))) {
            String targetStatus = String.valueOf(target.getOrDefault("order_status_code", ""));
            String appStatus = mapOrderStatus(String.valueOf(appOrder.getOrDefault("order_status_code", "")));
            boolean rejectedResolvedByUser = "REJECTED".equalsIgnoreCase(targetStatus)
                    && StringUtils.hasText(appStatus)
                    && !"REJECTED".equalsIgnoreCase(appStatus);
            if (!rejectedResolvedByUser) {
                return false;
            }
        }
        if (Boolean.TRUE.equals(target.get("_from_app"))) return true;

        Long targetUserId = InMemoryData.toLong(target.get("user_id"));
        if (!Objects.equals(targetUserId, appUserId)) return true;

        String targetOrderNo = String.valueOf(target.getOrDefault("order_no", ""));
        String appOrderNo = String.valueOf(appOrder.getOrDefault("order_no", ""));
        if (!Objects.equals(targetOrderNo, appOrderNo)) return true;

        String targetSubmitAt = String.valueOf(target.getOrDefault("submit_at", ""));
        String appSubmitAt = String.valueOf(appOrder.getOrDefault("submit_at", appOrder.getOrDefault("created_at", "")));
        if (!StringUtils.hasText(targetSubmitAt)) return true;
        return StringUtils.hasText(appSubmitAt) && appSubmitAt.compareTo(targetSubmitAt) >= 0;
    }

    private void syncPointLedgerFromMiniProgram(Long userId) {
        Map<String, Object> appUser = appStore.getUser(userId);
        String userName = appUser == null ? "积分用户" : String.valueOf(appUser.getOrDefault("nick_name", "积分用户"));
        for (Map<String, Object> appLedger : appStore.getPointLedgers(userId)) {
            Long id = InMemoryData.toLong(appLedger.get("id"));
            Map<String, Object> target = pointLedger.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.getOrDefault("app_ledger_id", 0L)), id))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                target = findById(pointLedger, id);
                if (target != null && !Objects.equals(InMemoryData.toLong(target.get("user_id")), userId)) {
                    target = null;
                }
            }
            if (target == null) {
                target = new LinkedHashMap<>();
                long finalId = (id == null || id <= 0 || findById(pointLedger, id) != null)
                        ? pointLedgerIdSeq.incrementAndGet()
                        : id;
                pointLedgerIdSeq.set(Math.max(pointLedgerIdSeq.get(), finalId));
                target.put("id", finalId);
                pointLedger.add(target);
            }
            target.put("user_id", userId);
            target.put("user_name", userName);
            target.put("app_ledger_id", id);
            target.put("biz_type_code", String.valueOf(appLedger.getOrDefault("biz_type_code", "")));
            target.put("change_amount", InMemoryData.toLong(appLedger.getOrDefault("change_amount", 0)));
            target.put("balance_after", InMemoryData.toLong(appLedger.getOrDefault("balance_after", 0)));
            target.put("occurred_at", String.valueOf(appLedger.getOrDefault("occurred_at", now())));
            String note = String.valueOf(appLedger.getOrDefault("note", "")).trim();
            if (!StringUtils.hasText(note)) {
                note = String.valueOf(appLedger.getOrDefault("remark", "")).trim();
            }
            target.put("note", note);
            target.put("consume_amount", toMoney(appLedger.getOrDefault("consume_amount", target.getOrDefault("consume_amount", 0D))));
            target.put("profit_change", toMoney(appLedger.getOrDefault("profit_change", target.getOrDefault("profit_change", 0D))));
            if (appLedger.containsKey("restored_flag")) {
                target.put("restored_flag", InMemoryData.toInt(appLedger.getOrDefault("restored_flag", 0)));
            }
            if (appLedger.containsKey("restored_at")) {
                target.put("restored_at", String.valueOf(appLedger.getOrDefault("restored_at", "")));
            }
            if (appLedger.containsKey("restore_of_ledger_id")) {
                target.put("restore_of_ledger_id", InMemoryData.toLong(appLedger.getOrDefault("restore_of_ledger_id", 0)));
            }
            if (appLedger.containsKey("restore_by_ledger_id")) {
                target.put("restore_by_ledger_id", InMemoryData.toLong(appLedger.getOrDefault("restore_by_ledger_id", 0)));
            }
        }
    }

    private List<Map<String, Object>> deduplicateOrderItems(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) return new ArrayList<>();
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> item : source) {
            if (item == null || item.isEmpty()) continue;
            String key = buildOrderItemDedupKey(item);
            unique.putIfAbsent(key, item);
        }
        return new ArrayList<>(unique.values());
    }

    private String buildOrderItemDedupKey(Map<String, Object> item) {
        long spuId = InMemoryData.toLong(item.getOrDefault("spu_id", item.getOrDefault("product_id", 0)));
        long skuId = InMemoryData.toLong(item.getOrDefault("sku_id", 0));
        String productName = String.valueOf(item.getOrDefault("spu_name", item.getOrDefault("product_name_snapshot", ""))).trim();
        String skuName = String.valueOf(item.getOrDefault("sku_name", item.getOrDefault("sku_name_snapshot", ""))).trim();
        long unitPoint = InMemoryData.toLong(item.getOrDefault("point_price", item.getOrDefault("unit_point_price", 0)));
        int quantity = Math.max(1, InMemoryData.toInt(item.getOrDefault("quantity", 1)));
        long totalPoint = InMemoryData.toLong(item.getOrDefault("total_point_amount", unitPoint * quantity));
        return spuId + "|" + skuId + "|" + productName + "|" + skuName + "|" + unitPoint + "|" + quantity + "|" + totalPoint;
    }

    private void syncAssetsFromMiniProgram(Long userId) {
        for (Map<String, Object> appAsset : appStore.listAssetsByUser(userId)) {
            Long assetId = InMemoryData.toLong(appAsset.get("id"));
            Map<String, Object> target = findById(backpackAssets, assetId);
            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("id", assetId);
                backpackAssets.add(target);
            }
            target.put("asset_no", String.valueOf(appAsset.getOrDefault("id", assetId)));
            target.put("user_name", String.valueOf(appStore.getUser(userId).getOrDefault("nick_name", "积分用户")));
            target.put("asset_name", String.valueOf(appAsset.getOrDefault("asset_name", "")));
            target.put("asset_type_code", String.valueOf(appAsset.getOrDefault("asset_type_code", "GROUP_QR")));
            target.put("status_code", String.valueOf(appAsset.getOrDefault("asset_status_code", "ACTIVE")));
            target.put("obtained_at", String.valueOf(appAsset.getOrDefault("created_at", now())));
            target.put("expire_at", String.valueOf(appAsset.getOrDefault("expire_at", "")));

            backpackFlows.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("asset_id")), assetId));
            for (Map<String, Object> appFlow : appStore.getAssetFlows(assetId)) {
                Map<String, Object> flow = new LinkedHashMap<>();
                flow.put("id", InMemoryData.toLong(appFlow.getOrDefault("id", 0)));
                flow.put("asset_id", assetId);
                flow.put("action_type_code", String.valueOf(appFlow.getOrDefault("action_code", "")));
                flow.put("action_text", String.valueOf(appFlow.getOrDefault("action_code", "")));
                flow.put("note", String.valueOf(appFlow.getOrDefault("remark", "")));
                flow.put("operator_name", "系统");
                flow.put("occurred_at", String.valueOf(appFlow.getOrDefault("occurred_at", now())));
                backpackFlows.add(flow);
            }
        }
    }

    private void syncGroupResourcesFromMiniProgram() {
        for (Map<String, Object> appResource : appStore.listGroupResources()) {
            Long id = InMemoryData.toLong(appResource.get("id"));
            if (groupResourceDeletedIds.contains(id)) {
                continue;
            }
            Map<String, Object> target = findById(groupResources, id);
            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("id", id);
                groupResources.add(target);
            } else if (Boolean.TRUE.equals(target.get("_sync_locked"))) {
                continue;
            }
            target.put("group_name", String.valueOf(appResource.getOrDefault("resource_name", "")));
            target.put("qr_image_url", String.valueOf(appResource.getOrDefault("qr_code_url", "")));
            target.put("intro_text", String.valueOf(appResource.getOrDefault("resource_name", "")));
            target.put("max_member_count", 500);
            target.put("current_member_count", 0);
            target.put("expire_at", "2099-12-31 23:59:59");
            target.put("status_code", "ACTIVE".equals(String.valueOf(appResource.getOrDefault("status_code", "ACTIVE"))) ? "ENABLED" : "DISABLED");
            target.put("updated_at", now());
        }
    }

    private String mapUserStatus(String value) {
        return "FROZEN".equalsIgnoreCase(value) ? "FROZEN" : "ACTIVE";
    }

    private String mapCategoryStatus(String value) {
        return "ACTIVE".equalsIgnoreCase(value) || "ENABLED".equalsIgnoreCase(value) ? "ENABLED" : "DISABLED";
    }

    private String mapCategoryStatusToApp(String adminStatus) {
        return "ENABLED".equalsIgnoreCase(adminStatus) ? "ACTIVE" : "DISABLED";
    }

    private String mapGroupStatusToApp(String adminStatus) {
        return "ENABLED".equalsIgnoreCase(adminStatus) ? "ACTIVE" : "DISABLED";
    }

    private void syncCategoryToMiniProgram(Map<String, Object> category) {
        if (appStore == null || category == null) return;
        Long categoryId = InMemoryData.toLong(category.get("id"));
        if (categoryId == null || categoryId <= 0) return;
        String categoryName = String.valueOf(category.getOrDefault("category_name", ""));
        Integer sortNo = InMemoryData.toInt(category.getOrDefault("sort_no", 100));
        String appStatus = mapCategoryStatusToApp(String.valueOf(category.getOrDefault("status_code", "ENABLED")));
        appStore.upsertCategoryFromAdmin(categoryId, categoryName, sortNo, appStatus);
    }

    private void syncMiniProgramDataFromAdminState() {
        if (appStore == null) return;
        normalizeDuplicateUsersFromMiniProgramStore();
        syncAppOrderSequencesFromAdminState();
        syncAllUsersAndPointsToMiniProgram();
        syncAllUserAddressesToMiniProgram();
        syncAllOrdersToMiniProgram();
        syncAllCategoriesToMiniProgram();
        syncAllSpusToMiniProgram();
        syncAllSpuCategoriesToMiniProgram();
        syncAllGroupResourcesToMiniProgram();
        syncAllProductMainImagesToMiniProgram();
        syncHomeBannersFromRecommendSlots();
    }

    private void normalizeDuplicateUsersFromMiniProgramStore() {
        if (appStore == null) return;
        Map<Long, Long> mergeMapping = appStore.normalizeDuplicateWechatUsers();
        if (mergeMapping == null || mergeMapping.isEmpty()) return;
        applyUserMergeMappingToAdminState(mergeMapping);
    }

    private void applyUserMergeMappingToAdminState(Map<Long, Long> mergeMapping) {
        if (mergeMapping == null || mergeMapping.isEmpty()) return;

        LinkedHashMap<Long, Long> finalMapping = new LinkedHashMap<>();
        mergeMapping.forEach((from, to) -> {
            Long finalTo = resolveMergedUserId(to, mergeMapping);
            if (from != null && finalTo != null && !Objects.equals(from, finalTo)) {
                finalMapping.put(from, finalTo);
            }
        });
        if (finalMapping.isEmpty()) return;

        LinkedHashMap<Long, Map<String, Object>> mergedUsers = new LinkedHashMap<>();
        for (Map<String, Object> item : users) {
            if (item == null) continue;
            Long rawId = InMemoryData.toLong(item.getOrDefault("id", 0));
            if (rawId == null || rawId <= 0) continue;
            Long userId = resolveMergedUserId(rawId, finalMapping);
            Map<String, Object> target = mergedUsers.get(userId);
            if (target == null) {
                target = new LinkedHashMap<>(item);
                target.put("id", userId);
                mergedUsers.put(userId, target);
                continue;
            }
            mergeAdminUserSummary(target, item);
        }
        users.clear();
        users.addAll(mergedUsers.values());

        remapUserIdField(userAddresses, "user_id", finalMapping);
        remapUserIdField(pointLedger, "user_id", finalMapping);
        remapUserIdField(orders, "user_id", finalMapping);
        remapUserIdField(backpackAssets, "user_id", finalMapping);
        remapUserIdField(wishDemands, "user_id", finalMapping);
    }

    private void remapUserIdField(List<Map<String, Object>> rows,
                                  String field,
                                  Map<Long, Long> mapping) {
        if (rows == null || rows.isEmpty() || !StringUtils.hasText(field) || mapping == null || mapping.isEmpty()) return;
        for (Map<String, Object> row : rows) {
            if (row == null) continue;
            Long rawId = InMemoryData.toLong(row.getOrDefault(field, 0));
            if (rawId == null || rawId <= 0) continue;
            Long mappedId = resolveMergedUserId(rawId, mapping);
            if (!Objects.equals(rawId, mappedId)) {
                row.put(field, mappedId);
            }
        }
    }

    private Long resolveMergedUserId(Long userId, Map<Long, Long> mapping) {
        if (userId == null || mapping == null || mapping.isEmpty()) return userId;
        Long cursor = userId;
        Set<Long> guard = new HashSet<>();
        while (cursor != null && mapping.containsKey(cursor) && guard.add(cursor)) {
            cursor = mapping.get(cursor);
        }
        return cursor == null ? userId : cursor;
    }

    private void mergeAdminUserSummary(Map<String, Object> target, Map<String, Object> source) {
        if (target == null || source == null) return;
        fillFieldIfBlank(target, "nick_name", source.get("nick_name"));
        fillFieldIfBlank(target, "phone", source.get("phone"));
        fillFieldIfBlank(target, "phone_masked", source.get("phone_masked"));
        fillFieldIfBlank(target, "open_id", source.get("open_id"));
        fillFieldIfBlank(target, "union_id", source.get("union_id"));
        fillFieldIfBlank(target, "avatar_url", source.get("avatar_url"));

        String targetStatus = String.valueOf(target.getOrDefault("user_status_code", "ACTIVE"));
        String sourceStatus = String.valueOf(source.getOrDefault("user_status_code", "ACTIVE"));
        if ("FROZEN".equalsIgnoreCase(targetStatus) || "FROZEN".equalsIgnoreCase(sourceStatus)) {
            target.put("user_status_code", "FROZEN");
        } else {
            target.put("user_status_code", "ACTIVE");
        }

        long targetBalance = InMemoryData.toLong(target.getOrDefault("point_balance", 0));
        long sourceBalance = InMemoryData.toLong(source.getOrDefault("point_balance", 0));
        target.put("point_balance", Math.max(targetBalance, sourceBalance));

        double targetConsume = toMoney(target.getOrDefault("total_consume_amount", 0D));
        double sourceConsume = toMoney(source.getOrDefault("total_consume_amount", 0D));
        target.put("total_consume_amount", toMoney(targetConsume + sourceConsume));

        double targetProfit = toMoney(target.getOrDefault("profit_amount", 0D));
        double sourceProfit = toMoney(source.getOrDefault("profit_amount", 0D));
        target.put("profit_amount", toMoney(targetProfit + sourceProfit));

        int targetOrderCount = InMemoryData.toInt(target.getOrDefault("order_count", 0));
        int sourceOrderCount = InMemoryData.toInt(source.getOrDefault("order_count", 0));
        target.put("order_count", Math.max(targetOrderCount, sourceOrderCount));

        String targetCreatedAt = String.valueOf(target.getOrDefault("created_at", ""));
        String sourceCreatedAt = String.valueOf(source.getOrDefault("created_at", ""));
        if (!StringUtils.hasText(targetCreatedAt) || (StringUtils.hasText(sourceCreatedAt) && sourceCreatedAt.compareTo(targetCreatedAt) < 0)) {
            target.put("created_at", sourceCreatedAt);
        }
    }

    private void fillFieldIfBlank(Map<String, Object> target, String field, Object sourceValue) {
        if (target == null || !StringUtils.hasText(field)) return;
        String current = String.valueOf(target.getOrDefault(field, "")).trim();
        if (StringUtils.hasText(current)) return;
        String next = String.valueOf(sourceValue == null ? "" : sourceValue).trim();
        if (!StringUtils.hasText(next)) return;
        target.put(field, next);
    }

    private String mapUserStatusToApp(String adminStatus) {
        return "FROZEN".equalsIgnoreCase(adminStatus) ? "FROZEN" : "ACTIVE";
    }

    private void syncSingleUserPointToMiniProgram(Long userId) {
        if (appStore == null || userId == null || userId <= 0) return;
        Map<String, Object> user = findById(users, userId);
        if (user == null) return;
        List<Map<String, Object>> ledgers = pointLedger.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.getOrDefault("user_id", 0)), userId))
                .map(this::copyMap)
                .collect(Collectors.toList());
        appStore.upsertUserPointFromAdmin(
                userId,
                String.valueOf(user.getOrDefault("nick_name", "")),
                String.valueOf(user.getOrDefault("phone_masked", "")),
                mapUserStatusToApp(String.valueOf(user.getOrDefault("user_status_code", "ACTIVE"))),
                InMemoryData.toLong(user.getOrDefault("point_balance", 0)),
                toMoney(user.getOrDefault("total_consume_amount", 0D)),
                toMoney(user.getOrDefault("profit_amount", 0D))
        );
        appStore.syncWechatIdentityFromAdmin(
                userId,
                String.valueOf(user.getOrDefault("open_id", "")),
                String.valueOf(user.getOrDefault("union_id", "")),
                String.valueOf(user.getOrDefault("phone", "")),
                String.valueOf(user.getOrDefault("phone_masked", "")),
                String.valueOf(user.getOrDefault("avatar_url", ""))
        );
        appStore.replacePointLedgersFromAdmin(userId, ledgers);
    }

    private void syncAllUsersAndPointsToMiniProgram() {
        Set<Long> userIds = users.stream()
                .map(item -> InMemoryData.toLong(item.getOrDefault("id", 0)))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long userId : userIds) {
            syncSingleUserPointToMiniProgram(userId);
        }
    }

    private void syncAllUserAddressesToMiniProgram() {
        Map<Long, List<Map<String, Object>>> grouped = userAddresses.stream()
                .collect(Collectors.groupingBy(
                        item -> InMemoryData.toLong(item.getOrDefault("user_id", 0)),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        grouped.forEach((userId, addressList) -> {
            if (userId == null || userId <= 0) return;
            List<Map<String, Object>> sorted = addressList.stream()
                    .sorted((a, b) -> Long.compare(InMemoryData.toLong(a.get("id")), InMemoryData.toLong(b.get("id"))))
                    .collect(Collectors.toList());
            appStore.replaceAddressesFromAdmin(userId, sorted);
        });
    }

    private void syncAllOrdersToMiniProgram() {
        for (Map<String, Object> order : orders) {
            Long orderId = InMemoryData.toLong(order.get("id"));
            if (orderId == null || orderId <= 0) continue;
            List<Map<String, Object>> items = orderItems.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                    .map(this::copyMap)
                    .collect(Collectors.toList());
            List<Map<String, Object>> flows = orderFlows.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                    .map(this::copyMap)
                    .collect(Collectors.toList());
            Map<String, Object> delivery = orderDeliveries.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                    .findFirst()
                    .map(this::copyMap)
                    .orElse(null);
            appStore.upsertOrderFromAdmin(copyMap(order), items, flows, delivery, null);
        }
    }

    private void syncOrderToMiniProgram(Long orderId) {
        if (appStore == null || orderId == null) return;
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return;
        List<Map<String, Object>> items = orderItems.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .map(this::copyMap)
                .collect(Collectors.toList());
        List<Map<String, Object>> flows = orderFlows.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .map(this::copyMap)
                .collect(Collectors.toList());
        Map<String, Object> delivery = orderDeliveries.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                .findFirst()
                .map(this::copyMap)
                .orElse(null);
        appStore.upsertOrderFromAdmin(copyMap(order), items, flows, delivery, null);
    }

    private void syncAppOrderSequencesFromAdminState() {
        long maxOrderId = orders.stream()
                .map(item -> InMemoryData.toLong(item.get("id")))
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        long maxOrderNo = orders.stream()
                .map(item -> parseOrderNoNumeric(String.valueOf(item.getOrDefault("order_no", ""))))
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        appStore.ensureOrderSequences(maxOrderId, maxOrderNo);
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

    private void syncHomeBannersFromRecommendSlots() {
        if (appStore == null) return;
        Set<Long> homeSlotIds = recommendSlots.stream()
                .filter(slot -> "ENABLED".equalsIgnoreCase(String.valueOf(slot.getOrDefault("status_code", "ENABLED"))))
                .filter(slot -> {
                    String code = String.valueOf(slot.getOrDefault("slot_code", "")).trim().toUpperCase(Locale.ROOT);
                    return "HOME_BANNER".equals(code) || "HOME_CAROUSEL".equals(code);
                })
                .map(slot -> InMemoryData.toLong(slot.get("id")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (homeSlotIds.isEmpty()) {
            appStore.replaceHomeBannersFromAdmin(new ArrayList<>());
            return;
        }

        Map<Long, String> productImageMap = appStore.listProducts().stream()
                .collect(Collectors.toMap(
                        p -> InMemoryData.toLong(p.get("id")),
                        p -> String.valueOf(p.getOrDefault("main_image_url", "")),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<String> bannerList = recommendItems.stream()
                .filter(item -> homeSlotIds.contains(InMemoryData.toLong(item.get("slot_id"))))
                .filter(item -> "ENABLED".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", "ENABLED"))))
                .sorted((a, b) -> Integer.compare(InMemoryData.toInt(b.get("sort_no")), InMemoryData.toInt(a.get("sort_no"))))
                .map(item -> {
                    String custom = resolveRecommendItemBannerUrl(item);
                    if (StringUtils.hasText(custom)) return custom;
                    return productImageMap.getOrDefault(InMemoryData.toLong(item.get("spu_id")), "");
                })
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        appStore.replaceHomeBannersFromAdmin(bannerList);
    }

    private boolean containsRecommendItemBannerUrlKey(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return false;
        return payload.containsKey("banner_image_url")
                || payload.containsKey("image_url")
                || payload.containsKey("banner_url")
                || payload.containsKey("cover_url")
                || payload.containsKey("main_image_url")
                || payload.containsKey("file_url");
    }

    private String resolveRecommendItemBannerUrl(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return "";
        List<String> keys = Arrays.asList("banner_image_url", "image_url", "banner_url", "cover_url", "main_image_url", "file_url");
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) continue;
            String normalized = normalizeInternalAdminFileUrl(String.valueOf(value));
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return "";
    }

    private void syncAllCategoriesToMiniProgram() {
        List<Map<String, Object>> adminCategories = cloneList(categories);
        Set<Long> adminIds = new LinkedHashSet<>();
        for (Map<String, Object> category : adminCategories) {
            Long categoryId = InMemoryData.toLong(category.get("id"));
            if (categoryId == null || categoryId <= 0) continue;
            adminIds.add(categoryId);
            syncCategoryToMiniProgram(category);
        }
        for (Long appId : appStore.listCategories().stream().map(item -> InMemoryData.toLong(item.get("id"))).collect(Collectors.toList())) {
            if (appId != null && appId > 0 && !adminIds.contains(appId)) {
                appStore.removeCategoryById(appId);
            }
        }
    }

    private void syncAllGroupResourcesToMiniProgram() {
        List<Map<String, Object>> adminGroupResources = cloneList(groupResources);
        Set<Long> adminIds = new LinkedHashSet<>();
        for (Map<String, Object> item : adminGroupResources) {
            Long id = InMemoryData.toLong(item.get("id"));
            if (id == null || id <= 0 || groupResourceDeletedIds.contains(id)) continue;
            adminIds.add(id);
            appStore.upsertGroupResourceFromAdmin(
                    id,
                    String.valueOf(item.getOrDefault("group_name", "")),
                    mapGroupStatusToApp(String.valueOf(item.getOrDefault("status_code", "ENABLED"))),
                    String.valueOf(item.getOrDefault("qr_image_url", ""))
            );
        }
        for (Long appId : appStore.listGroupResources().stream().map(item -> InMemoryData.toLong(item.get("id"))).collect(Collectors.toList())) {
            if (appId == null || appId <= 0) continue;
            if (!adminIds.contains(appId) || groupResourceDeletedIds.contains(appId)) {
                appStore.removeGroupResourceById(appId);
            }
        }
    }

    private void syncAllProductMainImagesToMiniProgram() {
        Set<Long> spuIds = new LinkedHashSet<>();
        for (Map<String, Object> spu : cloneList(spus)) {
            Long spuId = InMemoryData.toLong(spu.get("id"));
            if (spuId != null && spuId > 0) spuIds.add(spuId);
        }
        for (Map<String, Object> media : cloneList(medias)) {
            Long spuId = InMemoryData.toLong(media.get("spu_id"));
            if (spuId != null && spuId > 0) spuIds.add(spuId);
        }
        for (Long spuId : spuIds) {
            syncSpuMainImageToMiniProgram(spuId);
        }
    }

    private void syncAllSpusToMiniProgram() {
        if (appStore == null) return;
        List<Map<String, Object>> adminSpus = cloneList(spus);
        Set<Long> adminIds = new LinkedHashSet<>();
        for (Map<String, Object> spu : adminSpus) {
            Long spuId = InMemoryData.toLong(spu.get("id"));
            if (spuId == null || spuId <= 0) continue;
            adminIds.add(spuId);

            Long categoryId = InMemoryData.toLong(spu.get("category_id"));
            if (categoryId == null || categoryId <= 0) {
                categoryId = resolveCategoryIdByName(String.valueOf(spu.getOrDefault("category_name", "")));
            }
            List<Long> categoryIds = new ArrayList<>();
            Object rawCategoryIds = spu.get("category_ids");
            if (rawCategoryIds instanceof Collection<?>) {
                for (Object one : (Collection<?>) rawCategoryIds) {
                    Long cid = InMemoryData.toLong(one);
                    if (cid != null && cid > 0) categoryIds.add(cid);
                }
            }
            if (categoryIds.isEmpty() && categoryId != null && categoryId > 0) {
                categoryIds.add(categoryId);
            }
            String productName = String.valueOf(spu.getOrDefault("spu_name", ""));
            String saleStatus = String.valueOf(spu.getOrDefault("status_code", "OFF_SHELF"));
            Long pointPrice = InMemoryData.toLong(spu.getOrDefault("point_price_min", 0));
            if (pointPrice == null || pointPrice <= 0) {
                pointPrice = InMemoryData.toLong(spu.getOrDefault("point_price_max", 0));
            }
            Long stock = InMemoryData.toLong(spu.getOrDefault("total_stock", 0));
            String mainImage = resolveSpuMainImageFromAdmin(spuId);
            appStore.upsertProductFromAdmin(
                    spuId,
                    categoryId,
                    productName,
                    inferProductTypeCode(productName),
                    pointPrice == null ? 0L : pointPrice,
                    stock == null ? 0L : stock,
                    saleStatus,
                    mainImage,
                    spu.get("detail_html") == null ? "" : String.valueOf(spu.get("detail_html")),
                    5
            );
            List<Map<String, Object>> spuSkus = skus.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                    .map(this::copyMap)
                    .collect(Collectors.toList());
            appStore.upsertProductSkusFromAdmin(
                    spuId,
                    String.valueOf(spu.getOrDefault("skc_code", "")),
                    spuSkus
            );
            appStore.updateProductCategories(spuId, categoryIds);
        }

        List<Long> appIds = appStore.listProducts().stream()
                .map(item -> InMemoryData.toLong(item.get("id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        for (Long appId : appIds) {
            if (!adminIds.contains(appId)) {
                appStore.removeProductById(appId);
            }
        }
    }

    private String resolveSpuMainImageFromAdmin(Long spuId) {
        if (spuId == null) return "";
        Optional<Map<String, Object>> media = medias.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .filter(item -> "IMAGE".equalsIgnoreCase(String.valueOf(item.getOrDefault("media_type", "IMAGE"))))
                .sorted((a, b) -> {
                    int sortCmp = Integer.compare(InMemoryData.toInt(b.get("sort_no")), InMemoryData.toInt(a.get("sort_no")));
                    if (sortCmp != 0) return sortCmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .findFirst();
        if (media.isPresent()) {
            return String.valueOf(media.get().getOrDefault("media_url", ""));
        }
        return "";
    }

    private boolean ensureSpuCodes(Map<String, Object> spu) {
        if (spu == null) return false;
        Long id = InMemoryData.toLong(spu.get("id"));
        if (id == null || id <= 0) return false;
        String current = String.valueOf(spu.getOrDefault("skc_code", "")).trim();
        if (StringUtils.hasText(current)) return false;
        spu.put("skc_code", buildSkcCode(id));
        return true;
    }

    private boolean ensureSkuCodes(Map<String, Object> sku) {
        if (sku == null) return false;
        Long id = InMemoryData.toLong(sku.get("id"));
        if (id == null || id <= 0) return false;
        String current = String.valueOf(sku.getOrDefault("sku_code", "")).trim();
        if (StringUtils.hasText(current)) return false;
        sku.put("sku_code", buildSkuCode(id));
        return true;
    }

    private String buildSkcCode(long id) {
        long value = Math.max(0L, id);
        return "SKC" + String.format(Locale.ROOT, "%0" + SKC_CODE_DIGITS + "d", value);
    }

    private String buildSkuCode(long id) {
        long value = Math.max(0L, id);
        return "SKU" + String.format(Locale.ROOT, "%0" + SKU_CODE_DIGITS + "d", value);
    }

    private List<String> resolveSpuImageUrlsFromPayload(Map<String, Object> payload) {
        List<String> values = new ArrayList<>();
        if (payload == null || payload.isEmpty()) return values;
        values.addAll(extractProductImageValues(payload.get("image_urls")));
        values.addAll(extractProductImageValues(payload.get("cover_image_urls")));
        values.addAll(extractProductImageValues(payload.get("cover_image_url")));
        values.addAll(extractProductImageValues(payload.get("main_image_url")));
        values.addAll(extractProductImageValues(payload.get("media_url")));

        List<String> result = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            if (result.size() >= MAX_SPU_IMAGE_COUNT) break;
            String normalized = normalizeMediaUrl(value);
            if (!StringUtils.hasText(normalized) || !dedup.add(normalized)) continue;
            result.add(normalized);
        }
        return result;
    }

    private List<String> extractProductImageValues(Object rawValue) {
        List<String> result = new ArrayList<>();
        if (rawValue == null) return result;
        if (rawValue instanceof Collection<?>) {
            for (Object item : (Collection<?>) rawValue) {
                String text = String.valueOf(item == null ? "" : item).trim();
                if (StringUtils.hasText(text)) result.add(text);
            }
            return result;
        }
        if (rawValue.getClass().isArray()) {
            int length = Array.getLength(rawValue);
            for (int i = 0; i < length; i++) {
                String text = String.valueOf(Array.get(rawValue, i)).trim();
                if (StringUtils.hasText(text)) result.add(text);
            }
            return result;
        }
        String text = String.valueOf(rawValue).trim();
        if (!StringUtils.hasText(text)) return result;
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                List<Object> parsed = objectMapper.readValue(text, new TypeReference<List<Object>>() {});
                for (Object item : parsed) {
                    String value = String.valueOf(item == null ? "" : item).trim();
                    if (StringUtils.hasText(value)) result.add(value);
                }
                if (!result.isEmpty()) return result;
            } catch (Exception ignore) {
            }
        }
        result.add(text);
        return result;
    }

    private void createSpuImageMedias(Long spuId, List<String> imageUrls) {
        if (spuId == null || imageUrls == null || imageUrls.isEmpty()) return;
        List<String> source = imageUrls.stream()
                .filter(StringUtils::hasText)
                .limit(MAX_SPU_IMAGE_COUNT)
                .collect(Collectors.toList());
        if (source.isEmpty()) return;
        int sortNo = 1000;
        for (String imageUrl : source) {
            Map<String, Object> media = new LinkedHashMap<>();
            media.put("id", mediaIdSeq.incrementAndGet());
            media.put("spu_id", spuId);
            media.put("media_type", "IMAGE");
            media.put("media_url", imageUrl);
            media.put("sort_no", sortNo);
            sortNo = Math.max(1, sortNo - 10);
            medias.add(0, media);
        }
        enforceMaxImageMediaForSpu(spuId);
        syncSpuMainImageToMiniProgram(spuId);
    }

    private void createOrReplaceSpuMainImage(Long spuId, String mediaUrl) {
        if (spuId == null || !StringUtils.hasText(mediaUrl)) return;
        String normalizedUrl = normalizeMediaUrl(mediaUrl.trim());
        if (!StringUtils.hasText(normalizedUrl)) return;

        Map<String, Object> target = medias.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .filter(item -> "IMAGE".equalsIgnoreCase(String.valueOf(item.getOrDefault("media_type", "IMAGE"))))
                .sorted((a, b) -> {
                    int sortCmp = Integer.compare(InMemoryData.toInt(b.get("sort_no")), InMemoryData.toInt(a.get("sort_no")));
                    if (sortCmp != 0) return sortCmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .findFirst()
                .orElse(null);
        if (target == null) {
            target = new LinkedHashMap<>();
            target.put("id", mediaIdSeq.incrementAndGet());
            target.put("spu_id", spuId);
            medias.add(0, target);
        }
        target.put("media_type", "IMAGE");
        target.put("media_url", normalizedUrl);
        target.put("sort_no", 100);
        enforceMaxImageMediaForSpu(spuId);
        syncSpuMainImageToMiniProgram(spuId);
    }

    private String inferProductTypeCode(String productName) {
        String name = productName == null ? "" : productName;
        return (name.contains("入群") || name.contains("资格") || name.contains("权益") || name.contains("牛票"))
                ? "VIRTUAL"
                : "PHYSICAL";
    }

    private void syncAllSpuCategoriesToMiniProgram() {
        if (appStore == null) return;
        for (Map<String, Object> spu : cloneList(spus)) {
            Long spuId = InMemoryData.toLong(spu.get("id"));
            if (spuId == null || spuId <= 0) continue;
            List<Long> categoryIds = new ArrayList<>();
            Object rawCategoryIds = spu.get("category_ids");
            if (rawCategoryIds instanceof Collection<?>) {
                for (Object one : (Collection<?>) rawCategoryIds) {
                    Long id = InMemoryData.toLong(one);
                    if (id != null && id > 0) categoryIds.add(id);
                }
            }
            if (categoryIds.isEmpty()) {
                Long categoryId = resolveCategoryIdByName(String.valueOf(spu.getOrDefault("category_name", "")));
                if (categoryId != null && categoryId > 0) {
                    categoryIds.add(categoryId);
                }
            }
            if (categoryIds.isEmpty()) continue;
            Map<String, Object> liveSpu = findById(spus, spuId);
            if (liveSpu != null) {
                liveSpu.put("category_id", categoryIds.get(0));
                liveSpu.put("category_ids", new ArrayList<>(categoryIds));
            }
            appStore.updateProductCategories(spuId, categoryIds);
        }
    }

    private void syncSpuMainImageToMiniProgram(Long spuId) {
        if (appStore == null || spuId == null) return;
        Optional<Map<String, Object>> target = medias.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .filter(item -> "IMAGE".equalsIgnoreCase(String.valueOf(item.getOrDefault("media_type", "IMAGE"))))
                .filter(item -> StringUtils.hasText(String.valueOf(item.getOrDefault("media_url", ""))))
                .filter(item -> !String.valueOf(item.getOrDefault("media_url", "")).startsWith("data:"))
                .sorted((a, b) -> {
                    int sortCmp = Integer.compare(InMemoryData.toInt(b.get("sort_no")), InMemoryData.toInt(a.get("sort_no")));
                    if (sortCmp != 0) return sortCmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .findFirst();
        target.ifPresent(item -> appStore.updateProductMainImage(spuId, String.valueOf(item.get("media_url"))));
    }

    private boolean migrateLegacyDataUrlMedias(Long spuId) {
        if (spuId == null) return false;
        boolean changed = false;
        for (Map<String, Object> item : medias) {
            if (!Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId)) continue;
            String url = String.valueOf(item.getOrDefault("media_url", ""));
            if (!url.startsWith("data:")) continue;
            String normalized = normalizeMediaUrl(url);
            if (!Objects.equals(url, normalized)) {
                item.put("media_url", normalized);
                changed = true;
            }
        }
        return changed;
    }

    private boolean migrateLegacyDataUrlMediasToLocalStorage() {
        boolean changed = false;
        for (Map<String, Object> media : medias) {
            String url = String.valueOf(media.getOrDefault("media_url", ""));
            if (!url.startsWith("data:")) continue;
            String normalized = normalizeMediaUrl(url);
            if (!Objects.equals(url, normalized)) {
                media.put("media_url", normalized);
                changed = true;
            }
        }
        return changed;
    }

    private boolean normalizeAllProductStructures() {
        Set<Long> spuIds = new LinkedHashSet<>();
        for (Map<String, Object> spu : spus) {
            spuIds.add(InMemoryData.toLong(spu.get("id")));
        }
        for (Map<String, Object> sku : skus) {
            spuIds.add(InMemoryData.toLong(sku.get("spu_id")));
        }
        for (Map<String, Object> media : medias) {
            spuIds.add(InMemoryData.toLong(media.get("spu_id")));
        }
        boolean changed = false;
        for (Map<String, Object> spu : spus) {
            if (ensureSpuCodes(spu)) changed = true;
        }
        for (Map<String, Object> sku : skus) {
            if (ensureSkuCodes(sku)) changed = true;
        }
        for (Long spuId : spuIds) {
            if (enforceMaxSkuCountForSpu(spuId)) changed = true;
            if (enforceMaxImageMediaForSpu(spuId)) changed = true;
        }
        return changed;
    }

    private boolean enforceMaxSkuCountForSpu(Long spuId) {
        if (spuId == null) return false;
        List<Map<String, Object>> targetSkus = skus.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .sorted((a, b) -> Long.compare(InMemoryData.toLong(a.get("id")), InMemoryData.toLong(b.get("id"))))
                .collect(Collectors.toList());
        if (targetSkus.isEmpty()) return false;

        boolean changed = false;
        for (Map<String, Object> sku : targetSkus) {
            if (ensureSkuCodes(sku)) changed = true;
        }
        if (targetSkus.size() <= MAX_SPU_SKU_COUNT) {
            return changed;
        }

        Map<String, Object> keeper = targetSkus.get(MAX_SPU_SKU_COUNT - 1);
        long mergedStock = targetSkus.stream()
                .skip(MAX_SPU_SKU_COUNT)
                .mapToLong(item -> Math.max(0, InMemoryData.toLong(item.get("stock_available"))))
                .sum();
        if (mergedStock > 0) {
            long current = Math.max(0, InMemoryData.toLong(keeper.get("stock_available")));
            keeper.put("stock_available", current + mergedStock);
            changed = true;
        }

        Set<Long> removeIds = targetSkus.stream()
                .skip(MAX_SPU_SKU_COUNT)
                .map(item -> InMemoryData.toLong(item.get("id")))
                .collect(Collectors.toSet());
        if (!removeIds.isEmpty()) {
            changed = true;
        }
        skus.removeIf(item -> removeIds.contains(InMemoryData.toLong(item.get("id"))));
        syncSpu(spuId);
        return changed;
    }

    private boolean enforceMaxImageMediaForSpu(Long spuId) {
        if (spuId == null) return false;
        List<Map<String, Object>> imageMedias = medias.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .filter(item -> "IMAGE".equalsIgnoreCase(String.valueOf(item.getOrDefault("media_type", "IMAGE"))))
                .sorted((a, b) -> {
                    int sortCmp = Integer.compare(InMemoryData.toInt(b.get("sort_no")), InMemoryData.toInt(a.get("sort_no")));
                    if (sortCmp != 0) return sortCmp;
                    return Long.compare(InMemoryData.toLong(b.get("id")), InMemoryData.toLong(a.get("id")));
                })
                .collect(Collectors.toList());
        if (imageMedias.size() <= MAX_SPU_IMAGE_COUNT) return false;
        Set<Long> keeperIds = imageMedias.stream()
                .limit(MAX_SPU_IMAGE_COUNT)
                .map(item -> InMemoryData.toLong(item.get("id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        medias.removeIf(item ->
                Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId)
                        && "IMAGE".equalsIgnoreCase(String.valueOf(item.getOrDefault("media_type", "IMAGE")))
                        && !keeperIds.contains(InMemoryData.toLong(item.get("id")))
        );
        syncSpuMainImageToMiniProgram(spuId);
        return true;
    }

    private boolean normalizeStoredFileAndMediaUrls() {
        boolean changed = false;
        for (Map<String, Object> file : files) {
            Long fileId = InMemoryData.toLong(file.get("id"));
            String oldUrl = String.valueOf(file.getOrDefault("file_url", ""));
            String dataUrl = String.valueOf(file.getOrDefault("_file_data_url", ""));
            String localPath = String.valueOf(file.getOrDefault("_file_local_path", ""));
            String newUrl;
            if ((StringUtils.hasText(dataUrl) || StringUtils.hasText(localPath)) && fileId != null && fileId > 0) {
                newUrl = buildFileContentPath(fileId);
            } else {
                newUrl = normalizeInternalAdminFileUrl(oldUrl);
            }
            if (!Objects.equals(oldUrl, newUrl)) {
                file.put("file_url", newUrl);
                changed = true;
            }
        }

        for (Map<String, Object> media : medias) {
            String oldUrl = String.valueOf(media.getOrDefault("media_url", ""));
            String newUrl = normalizeInternalAdminFileUrl(oldUrl);
            if (!Objects.equals(oldUrl, newUrl)) {
                media.put("media_url", newUrl);
                changed = true;
            }
        }
        return changed;
    }

    private boolean normalizeRecommendItemBannerUrls() {
        boolean changed = false;
        for (Map<String, Object> item : recommendItems) {
            String currentBanner = normalizeInternalAdminFileUrl(String.valueOf(item.getOrDefault("banner_image_url", "")));
            if (!Objects.equals(String.valueOf(item.getOrDefault("banner_image_url", "")), currentBanner)) {
                item.put("banner_image_url", currentBanner);
                changed = true;
            }

            String resolved = resolveRecommendItemBannerUrl(item);
            if (!StringUtils.hasText(currentBanner) && StringUtils.hasText(resolved)) {
                item.put("banner_image_url", resolved);
                currentBanner = resolved;
                changed = true;
            }

            if (StringUtils.hasText(currentBanner)) {
                String legacyImageUrl = String.valueOf(item.getOrDefault("image_url", ""));
                if (!Objects.equals(legacyImageUrl, currentBanner)) {
                    item.put("image_url", currentBanner);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private String normalizeInternalAdminFileUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) return rawUrl;
        String url = rawUrl.trim();
        if (url.startsWith("/api/v1/admin/files/") && url.endsWith("/content")) {
            return url;
        }
        var matcher = ADMIN_FILE_CONTENT_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return rawUrl;
    }

    private String buildFileContentUrl(Long fileId) {
        return buildFileContentPath(fileId);
    }

    private String buildFileContentPath(Long fileId) {
        return "/api/v1/admin/files/" + fileId + "/content";
    }

    private String extractMimeTypeFromDataUrl(String dataUrl) {
        if (!StringUtils.hasText(dataUrl) || !dataUrl.startsWith("data:")) return "";
        int semi = dataUrl.indexOf(';');
        if (semi <= 5) return "";
        return dataUrl.substring(5, semi);
    }

    private String normalizeMediaUrl(String rawMediaUrl) {
        if (!StringUtils.hasText(rawMediaUrl) || !rawMediaUrl.startsWith("data:")) {
            return rawMediaUrl;
        }
        long fileId = fileIdSeq.incrementAndGet();
        String mime = extractMimeTypeFromDataUrl(rawMediaUrl);
        String fileName = "spu-media-" + fileId + guessImageExt(mime);
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("id", fileId);
        file.put("file_name", fileName);
        LocalStoredFile stored = storeDataUrlToLocalFile(fileId, fileName, rawMediaUrl, mime);
        if (stored != null) {
            file.put("_file_local_path", stored.relativePath);
            file.put("file_url", buildFileContentPath(fileId));
            file.put("mime_type", stored.mimeType);
            file.put("file_size_kb", stored.fileSizeKb);
        } else {
            file.put("_file_data_url", rawMediaUrl);
            file.put("file_url", buildFileContentPath(fileId));
            file.put("mime_type", StringUtils.hasText(mime) ? mime : "image/png");
            file.put("file_size_kb", Math.max(1, rawMediaUrl.length() / 1024));
        }
        file.put("uploaded_at", now());
        files.add(0, file);
        return String.valueOf(file.get("file_url"));
    }

    public synchronized String normalizePublicImageUrl(String rawImageUrl) {
        String raw = String.valueOf(rawImageUrl == null ? "" : rawImageUrl).trim();
        if (!StringUtils.hasText(raw)) return "";
        if (raw.startsWith("data:")) {
            return normalizeMediaUrl(raw);
        }
        return normalizeInternalAdminFileUrl(raw);
    }

    private boolean migrateLegacyDataUrlFilesToLocalStorage() {
        boolean changed = false;
        for (Map<String, Object> file : files) {
            Long fileId = InMemoryData.toLong(file.get("id"));
            if (fileId == null || fileId <= 0) continue;

            String localPath = String.valueOf(file.getOrDefault("_file_local_path", ""));
            Path localFile = resolveExistingStoredFilePath(localPath);
            if (StringUtils.hasText(localPath) && localFile != null && Files.exists(localFile) && Files.isRegularFile(localFile)) {
                continue;
            }

            String dataUrl = String.valueOf(file.getOrDefault("_file_data_url", ""));
            if (!StringUtils.hasText(dataUrl)) {
                String raw = String.valueOf(file.getOrDefault("file_url", ""));
                if (raw.startsWith("data:")) dataUrl = raw;
            }
            if (!StringUtils.hasText(dataUrl) || !dataUrl.startsWith("data:")) continue;

            String fileName = String.valueOf(file.getOrDefault("file_name", "legacy-" + fileId + ".png"));
            String mime = String.valueOf(file.getOrDefault("mime_type", ""));
            LocalStoredFile stored = storeDataUrlToLocalFile(fileId, fileName, dataUrl, mime);
            if (stored == null) continue;

            file.put("_file_local_path", stored.relativePath);
            file.put("file_url", buildFileContentPath(fileId));
            file.put("mime_type", stored.mimeType);
            file.put("file_size_kb", stored.fileSizeKb);
            file.remove("_file_data_url");
            changed = true;
        }
        return changed;
    }

    private Path resolveLocalStateSnapshotPath() {
        try {
            String configured = StringUtils.hasText(localAdminStateFile) ? localAdminStateFile : "./data/admin-business-state-v1.json";
            return resolveConfiguredStorageRoot(configured);
        } catch (Exception ex) {
            log.warn("解析本地状态快照路径失败: {}", ex.getMessage());
            return null;
        }
    }

    private Optional<String> loadSnapshotFromLocalFile() {
        Path snapshotPath = resolveLocalStateSnapshotPath();
        if (snapshotPath == null || !Files.exists(snapshotPath) || !Files.isRegularFile(snapshotPath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(snapshotPath, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(content)) return Optional.empty();
            return Optional.of(content);
        } catch (Exception ex) {
            log.warn("读取本地状态快照失败 path={}, err={}", snapshotPath, ex.getMessage());
            return Optional.empty();
        }
    }

    private void saveSnapshotToLocalFile(String snapshotJson) {
        if (snapshotJson == null) return;
        Path snapshotPath = resolveLocalStateSnapshotPath();
        if (snapshotPath == null) return;
        try {
            if (snapshotPath.getParent() != null) {
                Files.createDirectories(snapshotPath.getParent());
            }
            Files.writeString(
                    snapshotPath,
                    snapshotJson,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception ex) {
            log.warn("写入本地状态快照失败 path={}, err={}", snapshotPath, ex.getMessage());
        }
    }

    private Path ensureLocalFileStorageRoot() {
        Path cached = localFileStorageRoot;
        if (cached != null) return cached;
        synchronized (this) {
            if (localFileStorageRoot != null) return localFileStorageRoot;
            try {
                String configured = StringUtils.hasText(localFileStorageDir) ? localFileStorageDir : "./data/uploads";
                Path root = resolveConfiguredStorageRoot(configured);
                Files.createDirectories(root);
                localFileStorageRoot = root;
                log.info("本地文件存储目录: {}", root);
                return root;
            } catch (Exception ex) {
                log.warn("初始化本地文件存储目录失败，将回退DataURL存储: {}", ex.getMessage());
                return null;
            }
        }
    }

    private Path resolveConfiguredStorageRoot(String configured) {
        Path configuredPath = Paths.get(configured).normalize();
        if (configuredPath.isAbsolute()) {
            return configuredPath;
        }

        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        String dirName = userDir.getFileName() == null ? "" : userDir.getFileName().toString();
        if ("backend".equalsIgnoreCase(dirName)) {
            return userDir.resolve(configuredPath).normalize();
        }
        if ("mall-api".equalsIgnoreCase(dirName)) {
            Path parent = userDir.getParent();
            if (parent != null) {
                String parentName = parent.getFileName() == null ? "" : parent.getFileName().toString();
                if ("backend".equalsIgnoreCase(parentName)) {
                    return parent.resolve(configuredPath).normalize();
                }
            }
        }
        return userDir.resolve(configuredPath).normalize();
    }

    private Path resolveStoredFilePath(String localPath) {
        if (!StringUtils.hasText(localPath)) return null;
        Path root = ensureLocalFileStorageRoot();
        if (root == null) return null;
        try {
            Path raw = Paths.get(localPath.trim());
            Path target = raw.isAbsolute() ? raw.normalize() : root.resolve(raw).normalize();
            if (!target.startsWith(root)) return null;
            return target;
        } catch (Exception ex) {
            return null;
        }
    }

    private Path resolveExistingStoredFilePath(String localPath) {
        Path preferred = resolveStoredFilePath(localPath);
        if (preferred == null) return null;
        if (Files.exists(preferred) && Files.isRegularFile(preferred)) return preferred;

        Path raw;
        try {
            raw = Paths.get(localPath == null ? "" : localPath.trim());
        } catch (Exception ex) {
            return preferred;
        }
        if (raw.isAbsolute()) return preferred;

        for (Path root : resolveCandidateStorageRoots()) {
            if (root == null) continue;
            Path target = root.resolve(raw).normalize();
            if (!target.startsWith(root)) continue;
            if (Files.exists(target) && Files.isRegularFile(target)) {
                return target;
            }
        }
        return preferred;
    }

    private List<Path> resolveCandidateStorageRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        Path preferred = ensureLocalFileStorageRoot();
        if (preferred != null) roots.add(preferred);

        String configured = StringUtils.hasText(localFileStorageDir) ? localFileStorageDir : "./data/uploads";
        Path configuredPath;
        try {
            configuredPath = Paths.get(configured);
        } catch (Exception ex) {
            configuredPath = null;
        }
        if (configuredPath == null || configuredPath.isAbsolute()) {
            return new ArrayList<>(roots);
        }

        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path normalizedRel = configuredPath.normalize();

        roots.add(userDir.resolve(normalizedRel).normalize());
        roots.add(userDir.resolve("mall-api").resolve(normalizedRel).normalize());
        Path parent = userDir.getParent();
        if (parent != null) {
            roots.add(parent.resolve("mall-api").resolve(normalizedRel).normalize());
        }

        List<Path> result = new ArrayList<>();
        for (Path root : roots) {
            if (root == null) continue;
            if (preferred != null && root.equals(preferred)) continue;
            if (!Files.isDirectory(root)) continue;
            result.add(root);
        }
        return result;
    }

    private LocalStoredFile storeDataUrlToLocalFile(Long fileId, String fileName, String dataUrl, String preferredMime) {
        if (fileId == null || fileId <= 0 || !StringUtils.hasText(dataUrl) || !dataUrl.startsWith("data:")) return null;
        Path root = ensureLocalFileStorageRoot();
        if (root == null) return null;
        int split = dataUrl.indexOf(',');
        if (split < 0) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(split + 1));
            String dataMime = extractMimeTypeFromDataUrl(dataUrl);
            String mime = StringUtils.hasText(preferredMime) ? preferredMime : dataMime;
            if (!StringUtils.hasText(mime)) mime = "application/octet-stream";
            String ext = guessImageExt(mime);
            String relativePath = LocalDateTime.now(BEIJING_ZONE).format(FILE_DAY) + "/" + fileId + ext;
            Path target = root.resolve(relativePath).normalize();
            if (!target.startsWith(root)) return null;
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            long fileSizeKb = Math.max(1, (bytes.length + 1023L) / 1024L);
            return new LocalStoredFile(relativePath, mime, fileSizeKb);
        } catch (Exception ex) {
            log.warn("写入本地文件失败 fileId={}, fileName={}, err={}", fileId, fileName, ex.getMessage());
            return null;
        }
    }

    private MediaType safeParseMediaType(String mime) {
        if (!StringUtils.hasText(mime)) return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(mime);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private byte[] maybeTransformImage(byte[] source, String mime, Integer width, Integer quality) {
        if (source == null || source.length == 0) return source;
        if (!isResizableImageMime(mime)) return source;
        int safeWidth = width == null ? 0 : width;
        if (safeWidth < 120) return source;
        safeWidth = Math.min(safeWidth, 2048);
        int safeQuality = quality == null ? 76 : quality;
        safeQuality = Math.max(55, Math.min(90, safeQuality));
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(source));
            if (original == null) return source;
            int originalWidth = original.getWidth();
            int originalHeight = original.getHeight();
            if (originalWidth <= 0 || originalHeight <= 0 || safeWidth >= originalWidth) {
                return source;
            }
            int targetWidth = safeWidth;
            int targetHeight = (int) Math.max(1, Math.round(originalHeight * (targetWidth / (double) originalWidth)));

            int imageType = "image/jpeg".equalsIgnoreCase(mime) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
            BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            byte[] transformed;
            if ("image/jpeg".equalsIgnoreCase(mime)) {
                transformed = writeJpegWithQuality(resized, safeQuality / 100f);
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!ImageIO.write(resized, "png", out)) {
                    return source;
                }
                transformed = out.toByteArray();
            }
            return transformed.length > 0 ? transformed : source;
        } catch (Exception ex) {
            return source;
        }
    }

    private byte[] writeJpegWithQuality(BufferedImage image, float quality) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream fallback = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "jpg", fallback)) {
                return new byte[0];
            }
            return fallback.toByteArray();
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOut = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(imageOut);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.55f, Math.min(0.9f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private boolean isResizableImageMime(String mime) {
        if (!StringUtils.hasText(mime)) return false;
        String text = mime.toLowerCase(Locale.ROOT);
        return "image/jpeg".equals(text) || "image/png".equals(text);
    }

    private String guessMimeByFileName(String... names) {
        if (names == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        for (String raw : names) {
            if (!StringUtils.hasText(raw)) continue;
            String name = raw.trim().toLowerCase(Locale.ROOT);
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".gif")) return "image/gif";
            if (name.endsWith(".webp")) return "image/webp";
            if (name.endsWith(".svg")) return "image/svg+xml";
            if (name.endsWith(".bmp")) return "image/bmp";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String guessImageExt(String mime) {
        if (!StringUtils.hasText(mime)) return ".png";
        if ("image/jpeg".equalsIgnoreCase(mime)) return ".jpg";
        if ("image/gif".equalsIgnoreCase(mime)) return ".gif";
        if ("image/webp".equalsIgnoreCase(mime)) return ".webp";
        if ("image/png".equalsIgnoreCase(mime)) return ".png";
        return ".png";
    }

    private static class LocalStoredFile {
        private final String relativePath;
        private final String mimeType;
        private final long fileSizeKb;

        private LocalStoredFile(String relativePath, String mimeType, long fileSizeKb) {
            this.relativePath = relativePath;
            this.mimeType = mimeType;
            this.fileSizeKb = fileSizeKb;
        }
    }

    private String mapOrderStatus(String value) {
        if ("CANCELED".equalsIgnoreCase(value)) return "CLOSED";
        return value;
    }

    private String mapFlowAction(String actionCode) {
        if ("SUBMIT".equalsIgnoreCase(actionCode)) return "用户提交订单";
        if ("CANCEL".equalsIgnoreCase(actionCode)) return "用户取消订单";
        if ("USER_ACCEPT_REJECT".equalsIgnoreCase(actionCode)) return "用户接受驳回并转待发货";
        if ("USER_REJECT_REFUND".equalsIgnoreCase(actionCode)) return "用户拒绝并退回碎片";
        if ("APPROVE".equalsIgnoreCase(actionCode)) return "审核通过";
        if ("REJECT".equalsIgnoreCase(actionCode)) return "审核驳回";
        if ("SHIP".equalsIgnoreCase(actionCode)) return "订单发货";
        if ("COMPLETE".equalsIgnoreCase(actionCode)) return "订单完成";
        if ("CLOSE".equalsIgnoreCase(actionCode)) return "订单关闭";
        return StringUtils.hasText(actionCode) ? actionCode : "状态更新";
    }

    private void requireAdminLike(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录已过期，请重新登录");
        }
        String token = auth.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (jwtTokenService != null) {
            Long adminId = jwtTokenService.parseAdminAccessId(token);
            if (adminId != null && adminId > 0) {
                return;
            }
        }
        if (!allowLegacyToken || !token.startsWith("admin-token-")) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录已过期，请重新登录");
        }
    }

    private boolean normalizeAdminUserPasswords() {
        boolean changed = false;
        synchronized (adminUsers) {
            for (Map<String, Object> adminUser : adminUsers) {
                if (adminUser == null) continue;
                String old = String.valueOf(adminUser.getOrDefault("_password", ""));
                if (StringUtils.hasText(old)) continue;
                String username = String.valueOf(adminUser.getOrDefault("username", ""));
                adminUser.put("_password", buildDefaultAdminPassword(username));
                changed = true;
            }
        }
        return changed;
    }

    public String buildDefaultAdminPassword(String username) {
        if ("admin".equals(username)) return "admin123456";
        return StringUtils.hasText(username) ? username + "@123" : "Temp@123456";
    }

    public String resolveAdminUserPassword(Map<String, Object> adminUser) {
        if (adminUser == null) return "";
        String pwd = String.valueOf(adminUser.getOrDefault("_password", ""));
        if (StringUtils.hasText(pwd)) return pwd;
        return buildDefaultAdminPassword(String.valueOf(adminUser.getOrDefault("username", "")));
    }

    public synchronized Map<String, Object> authenticateAdmin(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) return null;
        Map<String, Object> admin = adminUsers.stream()
                .filter(item -> Objects.equals(String.valueOf(item.get("username")), username))
                .findFirst()
                .orElse(null);
        if (admin == null) return null;
        if (!matchAdminPassword(admin, password)) return null;
        return copyMap(admin);
    }

    public synchronized Map<String, Object> getAdminUserSafeById(Long adminId) {
        if (adminId == null) return null;
        Map<String, Object> admin = findById(adminUsers, adminId);
        return admin == null ? null : copyMap(admin);
    }

    public synchronized boolean updateAdminPassword(Long adminId, String oldPassword, String newPassword) {
        Map<String, Object> admin = findById(adminUsers, adminId);
        if (admin == null) return false;
        if (!matchAdminPassword(admin, oldPassword)) return false;
        admin.put("_password", newPassword);
        return true;
    }

    public synchronized boolean isSuperAdmin(Long adminId) {
        Map<String, Object> admin = findById(adminUsers, adminId);
        if (admin == null) return false;
        Object roleObj = admin.get("roles");
        if (!(roleObj instanceof Collection<?>)) return false;
        Collection<?> roles = (Collection<?>) roleObj;
        return roles.stream().map(String::valueOf).anyMatch("超级管理员"::equals);
    }

    public synchronized String getAdminPassword(Long adminId) {
        Map<String, Object> admin = findById(adminUsers, adminId);
        if (admin == null) return null;
        return resolveAdminUserPassword(admin);
    }

    public synchronized List<Map<String, Object>> snapshotUsers() {
        return cloneList(users);
    }

    public synchronized List<Map<String, Object>> snapshotOrders() {
        return cloneList(orders);
    }

    public synchronized List<Map<String, Object>> snapshotSpus() {
        return cloneList(spus);
    }

    public synchronized List<Map<String, Object>> snapshotSpuMedias(Long spuId) {
        if (spuId == null || spuId <= 0) return new ArrayList<>();
        return cloneList(medias).stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.getOrDefault("spu_id", 0)), spuId))
                .sorted((a, b) -> {
                    int sortCmp = Integer.compare(InMemoryData.toInt(b.getOrDefault("sort_no", 0)), InMemoryData.toInt(a.getOrDefault("sort_no", 0)));
                    if (sortCmp != 0) return sortCmp;
                    return Long.compare(InMemoryData.toLong(b.getOrDefault("id", 0)), InMemoryData.toLong(a.getOrDefault("id", 0)));
                })
                .map(this::copyMap)
                .collect(Collectors.toList());
    }

    public synchronized List<Map<String, Object>> snapshotPointLedger() {
        return cloneList(pointLedger);
    }

    public synchronized List<Map<String, Object>> snapshotOrderFlows() {
        return cloneList(orderFlows);
    }

    public synchronized List<Map<String, Object>> snapshotRecommendSlots() {
        return cloneList(recommendSlots);
    }

    public synchronized List<Map<String, Object>> snapshotRecommendItems() {
        return cloneList(recommendItems);
    }

    public synchronized List<Map<String, Object>> snapshotSystemConfigs() {
        return cloneList(systemConfigs);
    }

    public synchronized List<Map<String, Object>> snapshotWishDemands() {
        return cloneList(wishDemands);
    }

    public synchronized String generateDailyUserBalanceReport(String trigger) {
        if (userBalanceReportService == null) {
            return null;
        }
        return userBalanceReportService.generateDailyReport(snapshotUsers(), trigger);
    }

    private boolean refundOrderPoints(Long orderId, String bizTypeCode, String note) {
        if (orderId == null || orderId <= 0) return false;
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return false;
        if (Boolean.parseBoolean(String.valueOf(order.getOrDefault("point_refunded", false)))) return false;

        long userId = InMemoryData.toLong(order.get("user_id"));
        long amount = Math.max(0L, InMemoryData.toLong(order.get("total_point_amount")));
        if (amount <= 0 || userId <= 0) {
            order.put("point_refunded", true);
            return false;
        }
        Map<String, Object> user = findById(users, userId);
        if (user != null) {
            long after = InMemoryData.toLong(user.getOrDefault("point_balance", 0)) + amount;
            user.put("point_balance", after);
            Map<String, Object> ledger = new LinkedHashMap<>();
            ledger.put("id", pointLedgerIdSeq.incrementAndGet());
            ledger.put("user_id", userId);
            ledger.put("user_name", String.valueOf(user.getOrDefault("nick_name", "碎片用户")));
            ledger.put("biz_type_code", StringUtils.hasText(bizTypeCode) ? bizTypeCode : "ORDER_REFUND");
            ledger.put("change_amount", amount);
            ledger.put("balance_after", after);
            ledger.put("occurred_at", now());
            ledger.put("note", StringUtils.hasText(note) ? note : "订单返还碎片");
            pointLedger.add(0, ledger);
        }
        order.put("point_refunded", true);
        if (appStore != null) {
            appStore.refundOrderPointIfNeeded(orderId, bizTypeCode, note);
            syncSingleUserPointToMiniProgram(userId);
        }
        return true;
    }

    private List<Map<String, Object>> buildProcurementExportRows(String statusCode, String submitDate, String procurementStatus) {
        Map<Long, Map<String, Object>> deliveryByOrderId = orderDeliveries.stream()
                .collect(Collectors.toMap(
                        item -> InMemoryData.toLong(item.getOrDefault("order_id", 0)),
                        this::copyMap,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        String safeStatus = statusCode == null ? "" : statusCode.trim();
        String safeDate = submitDate == null ? "" : submitDate.trim();
        String safeProcurementStatus = procurementStatus == null ? "" : procurementStatus.trim();

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> sortedOrders = orders.stream()
                .filter(item -> !StringUtils.hasText(safeStatus) || safeStatus.equalsIgnoreCase(String.valueOf(item.getOrDefault("order_status_code", ""))))
                .filter(item -> !StringUtils.hasText(safeDate) || String.valueOf(item.getOrDefault("submit_at", "")).startsWith(safeDate))
                .filter(item -> !StringUtils.hasText(safeProcurementStatus) || matchesProcurementStatus(item, safeProcurementStatus))
                .sorted((a, b) -> String.valueOf(b.getOrDefault("submit_at", "")).compareTo(String.valueOf(a.getOrDefault("submit_at", ""))))
                .collect(Collectors.toList());

        for (Map<String, Object> order : sortedOrders) {
            ensureOrderProcurementFields(order);
            String orderStatusCode = String.valueOf(order.getOrDefault("order_status_code", ""));
            String orderStatusLabel = orderStatusText(orderStatusCode);
            String orderProcurementStatus = String.valueOf(order.getOrDefault("procurement_status", "PENDING_PROCURE"));
            String orderProcurementText = procurementStatusText(orderProcurementStatus);
            String userRemark = resolveOrderUserRemark(order);
            Long orderId = InMemoryData.toLong(order.get("id"));
            if (orderId == null || orderId <= 0) continue;
            Map<String, Object> delivery = deliveryByOrderId.get(orderId);
            Map<String, Object> appAddress = appStore == null ? null : appStore.getOrderAddressSnapshot(orderId);

            String receiverName = firstText(
                    delivery == null ? "" : String.valueOf(delivery.getOrDefault("receiver_name", "")),
                    appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("receiver_name", ""))
            );
            String receiverPhone = firstText(
                    delivery == null ? "" : String.valueOf(delivery.getOrDefault("receiver_phone", "")),
                    appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("receiver_phone", ""))
            );
            String provinceName = firstText(
                    delivery == null ? "" : String.valueOf(delivery.getOrDefault("province_name", "")),
                    appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("province_name", ""))
            );
            String cityName = firstText(
                    delivery == null ? "" : String.valueOf(delivery.getOrDefault("city_name", "")),
                    appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("city_name", ""))
            );
            String districtName = firstText(
                    delivery == null ? "" : String.valueOf(delivery.getOrDefault("district_name", "")),
                    appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("district_name", ""))
            );
            String receiverAddress = firstText(
                    delivery == null ? "" : String.valueOf(delivery.getOrDefault("receiver_address", "")),
                    appAddress == null ? "" : String.valueOf(appAddress.getOrDefault("detail_address", ""))
            );

            String fullReceiverAddress = buildFullAddress(provinceName, cityName, districtName, receiverAddress);
            String buyerFullInfo = buildBuyerFullInfo(receiverName, receiverPhone, fullReceiverAddress);
            List<Map<String, Object>> items = orderItems.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.getOrDefault("order_id", 0)), orderId))
                    .collect(Collectors.toList());
            if (items.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("order_no", String.valueOf(order.getOrDefault("order_no", "")));
                row.put("user_id", InMemoryData.toLong(order.getOrDefault("user_id", 0)));
                row.put("order_status_code", orderStatusCode);
                row.put("order_status_text", orderStatusLabel);
                row.put("procurement_status", orderProcurementStatus);
                row.put("procurement_status_text", orderProcurementText);
                row.put("sku_name", "");
                row.put("quantity", 0);
                row.put("product_sku_qty", "");
                row.put("point_price", 0);
                row.put("user_remark", userRemark);
                row.put("buyer_name", receiverName);
                row.put("receiver_phone", receiverPhone);
                row.put("province_name", provinceName);
                row.put("city_name", cityName);
                row.put("district_name", districtName);
                row.put("receiver_address", fullReceiverAddress);
                row.put("buyer_full_info", buyerFullInfo);
                row.put("submit_at", String.valueOf(order.getOrDefault("submit_at", "")));
                rows.add(row);
                continue;
            }
            for (Map<String, Object> item : items) {
                String productName = String.valueOf(item.getOrDefault("spu_name", item.getOrDefault("product_name_snapshot", "")));
                String skuName = String.valueOf(item.getOrDefault("sku_name", item.getOrDefault("sku_name_snapshot", "")));
                int quantity = InMemoryData.toInt(item.getOrDefault("quantity", 0));
                long pointPrice = InMemoryData.toLong(item.getOrDefault("point_price", 0));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("order_no", String.valueOf(order.getOrDefault("order_no", "")));
                row.put("user_id", InMemoryData.toLong(order.getOrDefault("user_id", 0)));
                row.put("order_status_code", orderStatusCode);
                row.put("order_status_text", orderStatusLabel);
                row.put("procurement_status", orderProcurementStatus);
                row.put("procurement_status_text", orderProcurementText);
                row.put("sku_name", skuName);
                row.put("quantity", quantity);
                row.put("product_sku_qty", buildProductSkuQtyText(productName, skuName, quantity));
                row.put("point_price", pointPrice);
                row.put("user_remark", userRemark);
                row.put("buyer_name", receiverName);
                row.put("receiver_phone", receiverPhone);
                row.put("province_name", provinceName);
                row.put("city_name", cityName);
                row.put("district_name", districtName);
                row.put("receiver_address", fullReceiverAddress);
                row.put("buyer_full_info", buyerFullInfo);
                row.put("submit_at", String.valueOf(order.getOrDefault("submit_at", "")));
                rows.add(row);
            }
        }
        return rows;
    }

    private String buildOrderGoodsSummary(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return "-";
        return items.stream()
                .map(item -> {
                    String productName = String.valueOf(item.getOrDefault("spu_name", item.getOrDefault("product_name_snapshot", "")));
                    String skuName = String.valueOf(item.getOrDefault("sku_name", item.getOrDefault("sku_name_snapshot", "")));
                    int quantity = InMemoryData.toInt(item.getOrDefault("quantity", 0));
                    return buildProductSkuQtyText(productName, skuName, quantity);
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("；"));
    }

    private String buildProductSkuQtyText(String productName, String skuName, int quantity) {
        String name = StringUtils.hasText(productName) ? productName.trim() : "商品";
        String sku = StringUtils.hasText(skuName) ? skuName.trim() : "";
        int safeQty = Math.max(1, quantity);
        if (StringUtils.hasText(sku)) {
            String normalizedSku = sku.replaceAll("\\s+", "");
            String normalizedName = name.replaceAll("\\s+", "");
            if ("默认规格".equals(sku) || normalizedSku.equalsIgnoreCase(normalizedName)) {
                return name + " x" + safeQty;
            }
            return name + " " + sku + " x" + safeQty;
        }
        return name + " x" + safeQty;
    }

    private String buildBuyerDisplay(String buyerName, String phoneFull) {
        String name = StringUtils.hasText(buyerName) ? buyerName.trim() : "匿名买家";
        String phone = String.valueOf(phoneFull == null ? "" : phoneFull).replaceAll("\\s+", "");
        if (!StringUtils.hasText(phone)) return name;
        return name + phone;
    }

    private String buildBuyerFullInfo(String buyerName, String buyerPhone, String fullAddress) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(buyerName)) sb.append(buyerName.trim());
        if (StringUtils.hasText(buyerPhone)) sb.append(buyerPhone.trim());
        if (StringUtils.hasText(fullAddress)) sb.append(fullAddress);
        return sb.toString();
    }

    private String buildFullAddress(String provinceName, String cityName, String districtName, String detailAddress) {
        String area = Stream.of(provinceName, cityName, districtName)
                .map(item -> String.valueOf(item == null ? "" : item).trim())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());
        String detail = String.valueOf(detailAddress == null ? "" : detailAddress).trim();
        if (!StringUtils.hasText(area)) return detail;
        if (!StringUtils.hasText(detail) || detail.startsWith(area)) return detail;
        return area + detail;
    }

    private String firstText(String... values) {
        if (values == null || values.length == 0) return "";
        for (String value : values) {
            String text = String.valueOf(value == null ? "" : value).trim();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return "";
    }

    private void ensureUserExtraFields(Map<String, Object> user) {
        if (user == null) return;
        user.put("backpack_id", formatBackpackId(user.get("id")));
        user.put("total_consume_amount", toMoney(user.getOrDefault("total_consume_amount", 0D)));
        user.put("profit_amount", toMoney(user.getOrDefault("profit_amount", 0D)));
    }

    private String formatBackpackId(Object rawUserId) {
        long id = Math.max(0L, InMemoryData.toLong(rawUserId));
        if (id <= 0L) return "01000";
        return String.format(Locale.ROOT, "%05d", id);
    }

    private double toMoney(Object value) {
        double raw = 0D;
        if (value instanceof Number) {
            raw = ((Number) value).doubleValue();
        } else if (value != null) {
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text)) {
                try {
                    raw = Double.parseDouble(text);
                } catch (Exception ignore) {
                    raw = 0D;
                }
            }
        }
        if (Double.isNaN(raw) || Double.isInfinite(raw)) raw = 0D;
        return Math.round(raw * 100D) / 100D;
    }

    private Map<String, Object> parseExtJsonMap(Object rawValue) {
        if (rawValue instanceof Map<?, ?>) {
            Map<String, Object> parsed = new LinkedHashMap<>();
            ((Map<?, ?>) rawValue).forEach((k, v) -> parsed.put(String.valueOf(k), v));
            return parsed;
        }
        String text = String.valueOf(rawValue == null ? "" : rawValue).trim();
        if (!StringUtils.hasText(text) || "null".equalsIgnoreCase(text)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignore) {
            return new LinkedHashMap<>();
        }
    }

    private String moneyText(double value) {
        return String.format(Locale.ROOT, "%.2f", toMoney(value));
    }

    private Map<String, Object> decideWishDemandInternal(Long wishId, String decision, String note, String operatorName) {
        Map<String, Object> target = findById(wishDemands, wishId);
        if (target == null) return null;
        String current = String.valueOf(target.getOrDefault("status_code", "PENDING")).toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(current)) return null;

        String safeDecision = String.valueOf(decision == null ? "" : decision).trim().toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(safeDecision) && !"REJECTED".equals(safeDecision)) {
            return null;
        }
        target.put("status_code", safeDecision);
        target.put("status_text", wishDemandStatusText(safeDecision));
        target.put("decision_note", String.valueOf(note == null ? "" : note).trim());
        target.put("decided_at", now());
        target.put("decided_by", StringUtils.hasText(operatorName) ? operatorName : "管理员");
        target.put("updated_at", now());
        if ("APPROVED".equals(safeDecision)) {
            target.put("notify_content", StringUtils.hasText(note)
                    ? "您的意向商品已确认上架，备注：" + note
                    : "您的意向商品已确认上架");
        } else {
            target.put("notify_content", StringUtils.hasText(note)
                    ? "您的意向商品暂不通过，原因：" + note
                    : "您的意向商品暂不通过，感谢您的建议");
        }
        return copyMap(target);
    }

    private String wishDemandStatusText(String status) {
        String key = String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(key)) return "已确认";
        if ("REJECTED".equals(key)) return "已拒绝";
        return "待处理";
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.contains(",") && !text.contains("\"") && !text.contains("\n") && !text.contains("\r")) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private boolean matchAdminPassword(Map<String, Object> admin, String password) {
        if (admin == null || !StringUtils.hasText(password)) return false;
        String expected = resolveAdminUserPassword(admin);
        if (Objects.equals(expected, password)) return true;
        String username = String.valueOf(admin.getOrDefault("username", ""));
        return Objects.equals(buildDefaultAdminPassword(username), password);
    }

    private boolean normalizeOrderProcurementStates() {
        boolean changed = false;
        synchronized (orders) {
            for (Map<String, Object> order : orders) {
                if (order == null) continue;
                String original = String.valueOf(order.getOrDefault("procurement_status", "")).trim();
                ensureOrderProcurementFields(order);
                String current = String.valueOf(order.getOrDefault("procurement_status", "")).trim();
                if (!Objects.equals(original, current)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void ensureOrderProcurementFields(Map<String, Object> order) {
        if (order == null) return;
        String normalized = normalizeProcurementStatus(String.valueOf(order.getOrDefault("procurement_status", "")));
        if (!StringUtils.hasText(normalized)) {
            normalized = "PENDING_PROCURE";
        }
        order.put("procurement_status", normalized);
        order.put("procurement_status_text", procurementStatusText(normalized));
        if ("PROCURED".equalsIgnoreCase(normalized)) {
            String procuredAt = String.valueOf(order.getOrDefault("procured_at", ""));
            if (!StringUtils.hasText(procuredAt)) {
                order.put("procured_at", now());
            }
            if (!StringUtils.hasText(String.valueOf(order.getOrDefault("procured_by", "")))) {
                order.put("procured_by", "系统管理员");
            }
        } else {
            if (!order.containsKey("procured_at")) order.put("procured_at", "");
            if (!order.containsKey("procured_by")) order.put("procured_by", "");
        }
    }

    private boolean matchesProcurementStatus(Map<String, Object> order, String status) {
        ensureOrderProcurementFields(order);
        String current = String.valueOf(order.getOrDefault("procurement_status", ""));
        String expected = normalizeProcurementStatus(status);
        if (!StringUtils.hasText(expected)) return true;
        return expected.equalsIgnoreCase(current);
    }

    private String normalizeProcurementStatus(String status) {
        if (!StringUtils.hasText(status)) return "";
        String safe = status.trim().toUpperCase(Locale.ROOT);
        if ("PROCURED".equals(safe) || "PURCHASED".equals(safe) || "DONE".equals(safe) || "Y".equals(safe) || "TRUE".equals(safe)) {
            return "PROCURED";
        }
        if ("PENDING_PROCURE".equals(safe) || "PENDING".equals(safe) || "WAIT".equals(safe) || "N".equals(safe) || "FALSE".equals(safe)) {
            return "PENDING_PROCURE";
        }
        return "";
    }

    private String procurementStatusText(String status) {
        String safe = normalizeProcurementStatus(status);
        if ("PROCURED".equals(safe)) return "已采购";
        return "待采购";
    }

    private String orderStatusText(String status) {
        String safe = StringUtils.hasText(status) ? mapOrderStatus(status.trim()).toUpperCase(Locale.ROOT) : "";
        if (StringUtils.hasText(safe)) {
            String dictText = dictItems.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> "ORDER_STATUS".equalsIgnoreCase(String.valueOf(item.getOrDefault("dict_type_code", ""))))
                    .filter(item -> {
                        String itemCode = String.valueOf(item.getOrDefault("item_code", ""));
                        String itemValue = String.valueOf(item.getOrDefault("item_value", ""));
                        return safe.equalsIgnoreCase(itemCode) || safe.equalsIgnoreCase(itemValue);
                    })
                    .map(item -> String.valueOf(item.getOrDefault("item_name", "")))
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse("");
            if (StringUtils.hasText(dictText)) return dictText;
        }
        if ("PENDING_AUDIT".equals(safe)) return "待审核";
        if ("PENDING_SHIP".equals(safe)) return "待发货";
        if ("SHIPPED".equals(safe)) return "已发货";
        if ("FINISHED".equals(safe)) return "已完成";
        if ("CLOSED".equals(safe)) return "已关闭";
        if ("REJECTED".equals(safe)) return "已驳回";
        if ("CANCELED".equals(safe)) return "已取消";
        return StringUtils.hasText(status) ? status : "-";
    }

    private String resolveOrderUserRemark(Map<String, Object> order) {
        if (order == null) return "";
        String userRemark = String.valueOf(order.getOrDefault("user_remark", "")).trim();
        if (StringUtils.hasText(userRemark)) return userRemark;
        return String.valueOf(order.getOrDefault("remark", "")).trim();
    }

    private boolean changeOrderStatus(Long orderId, String expectedStatus, String toStatus, String actionText, String note) {
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return false;
        String current = String.valueOf(order.get("order_status_code"));
        if (!Objects.equals(current, expectedStatus)) return false;
        order.put("order_status_code", toStatus);
        order.put("_order_admin_touched", true);
        Map<String, Object> flow = orderFlow(orderFlowIdSeq.incrementAndGet(), orderId, current, toStatus, actionText, note, "系统管理员", now());
        orderFlows.add(flow);
        syncOrderToMiniProgram(orderId);
        return true;
    }

    public synchronized int autoCompleteSignedOrders() {
        LocalDateTime threshold = LocalDateTime.now(BEIJING_ZONE).minusDays(3);
        List<Long> candidates = orders.stream()
                .filter(order -> "SHIPPED".equalsIgnoreCase(String.valueOf(order.getOrDefault("order_status_code", ""))))
                .map(order -> InMemoryData.toLong(order.get("id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int completed = 0;
        for (Long orderId : candidates) {
            Map<String, Object> delivery = orderDeliveries.stream()
                    .filter(item -> Objects.equals(InMemoryData.toLong(item.get("order_id")), orderId))
                    .findFirst()
                    .orElse(null);
            if (delivery == null) continue;
            String signedAt = String.valueOf(delivery.getOrDefault("signed_at", "")).trim();
            LocalDateTime signedTime = parseDateTime(signedAt);
            if (signedTime == null || signedTime.isAfter(threshold)) continue;
            if (!changeOrderStatus(orderId, "SHIPPED", "FINISHED", "自动完成订单", "物流签收满3天自动完成")) {
                continue;
            }
            Map<String, Object> order = findById(orders, orderId);
            if (order != null) {
                order.put("finish_at", now());
                order.put("buyer_decision_required", false);
            }
            syncOrderToMiniProgram(orderId);
            completed++;
        }
        if (completed > 0) {
            persistStateToMysql();
            log.info("签收满3天自动完成订单：{} 笔", completed);
        }
        return completed;
    }

    private synchronized int applyKdniaoCallback(String requestData) {
        if (kdniaoLogisticsService == null) return 0;
        KdniaoLogisticsService.CallbackPayload payload = kdniaoLogisticsService.parseCallback(requestData);
        if (payload.getEvents().isEmpty()) return 0;

        Set<Long> touchedOrderIds = new LinkedHashSet<>();
        for (KdniaoLogisticsService.CallbackTraceEvent event : payload.getEvents()) {
            String logisticCode = String.valueOf(event.getLogisticCode()).trim();
            if (!StringUtils.hasText(logisticCode)) continue;
            String shipperCode = String.valueOf(event.getShipperCode()).trim();
            String state = String.valueOf(event.getState()).trim();
            String statusCode = kdniaoLogisticsService.mapStateToDeliveryStatus(state);
            String statusText = kdniaoLogisticsService.mapStateToText(state);
            String latestTime = normalizeDateTimeText(event.getLatestAcceptTime());
            if (!StringUtils.hasText(latestTime)) latestTime = now();
            String latestStation = String.valueOf(event.getLatestAcceptStation());
            if (!StringUtils.hasText(latestStation)) {
                latestStation = String.valueOf(event.getReason());
            }
            List<Map<String, Object>> callbackTraceList = convertTraceNodesToList(event.getTraces());
            if (!callbackTraceList.isEmpty()) {
                String firstTime = String.valueOf(callbackTraceList.get(0).getOrDefault("accept_time", ""));
                String firstStation = String.valueOf(callbackTraceList.get(0).getOrDefault("accept_station", ""));
                if (StringUtils.hasText(firstTime)) latestTime = firstTime;
                if (StringUtils.hasText(firstStation)) latestStation = firstStation;
            }

            List<Map<String, Object>> matchedDeliveries = orderDeliveries.stream()
                    .filter(item -> Objects.equals(String.valueOf(item.getOrDefault("express_no", "")).trim(), logisticCode))
                    .filter(item -> {
                        String storedShipper = String.valueOf(item.getOrDefault("shipper_code", "")).trim();
                        return !StringUtils.hasText(shipperCode) || !StringUtils.hasText(storedShipper) || Objects.equals(storedShipper, shipperCode);
                    })
                    .collect(Collectors.toList());
            for (Map<String, Object> delivery : matchedDeliveries) {
                Long orderId = InMemoryData.toLong(delivery.get("order_id"));
                if (orderId == null || orderId <= 0) continue;
                delivery.put("delivery_status_code", statusCode);
                delivery.put("delivery_status_text", statusText);
                delivery.put("latest_trace_time", latestTime);
                delivery.put("latest_trace_station", latestStation);
                delivery.put("shipper_code", StringUtils.hasText(shipperCode) ? shipperCode : String.valueOf(delivery.getOrDefault("shipper_code", "")));
                if (!callbackTraceList.isEmpty()) {
                    delivery.put("trace_list", callbackTraceList);
                }

                if ("SIGNED".equalsIgnoreCase(statusCode)) {
                    String oldSignedAt = String.valueOf(delivery.getOrDefault("signed_at", ""));
                    if (!StringUtils.hasText(oldSignedAt)) {
                        delivery.put("signed_at", latestTime);
                        Map<String, Object> flow = orderFlow(orderFlowIdSeq.incrementAndGet(), orderId, "SHIPPED", "SHIPPED",
                                "物流签收", firstNotBlank(latestStation, "物流已签收"), "物流系统", now());
                        orderFlows.add(flow);
                    }
                }
                Map<String, Object> order = findById(orders, orderId);
                if (order != null) {
                    order.put("_order_admin_touched", true);
                    order.put("delivery_status_code", statusCode);
                }
                touchedOrderIds.add(orderId);
            }
        }

        if (!touchedOrderIds.isEmpty()) {
            touchedOrderIds.forEach(this::syncOrderToMiniProgram);
            persistStateToMysql();
        }
        return touchedOrderIds.size();
    }

    private Map<String, Object> buildKdniaoCallbackAck(boolean success, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("EBusinessID", kdniaoLogisticsService == null ? "" : kdniaoLogisticsService.getBusinessId());
        result.put("UpdateTime", now());
        result.put("Success", success);
        result.put("Reason", StringUtils.hasText(reason) ? reason : "");
        return result;
    }

    private String findOrderNo(Long orderId) {
        Map<String, Object> order = findById(orders, orderId);
        if (order == null) return "";
        return String.valueOf(order.getOrDefault("order_no", ""));
    }

    private void appendAssetFlow(Long assetId, String actionTypeCode, String actionText, String note) {
        Map<String, Object> flow = assetFlow(assetFlowIdSeq.incrementAndGet(), assetId, actionTypeCode, actionText, note, "系统管理员", now());
        backpackFlows.add(0, flow);
    }

    private void syncSpu(Long spuId) {
        Map<String, Object> spu = findById(spus, spuId);
        if (spu == null) return;
        ensureSpuCodes(spu);

        List<Map<String, Object>> spuSkus = skus.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("spu_id")), spuId))
                .collect(Collectors.toList());
        for (Map<String, Object> sku : spuSkus) {
            ensureSkuCodes(sku);
        }

        List<Map<String, Object>> enabledSkus = spuSkus.stream()
                .filter(item -> Objects.equals(String.valueOf(item.get("status_code")), "ENABLED"))
                .collect(Collectors.toList());

        long totalStock = enabledSkus.stream().mapToLong(item -> InMemoryData.toLong(item.get("stock_available"))).sum();
        spu.put("total_stock", totalStock);
        if (!enabledSkus.isEmpty()) {
            long minPrice = enabledSkus.stream().mapToLong(item -> InMemoryData.toLong(item.get("point_price"))).min().orElse(0L);
            long maxPrice = enabledSkus.stream().mapToLong(item -> InMemoryData.toLong(item.get("point_price"))).max().orElse(0L);
            spu.put("point_price_min", minPrice);
            spu.put("point_price_max", maxPrice);
        }
        spu.put("updated_at", now());
    }

    private Map<String, Object> findById(List<Map<String, Object>> list, Long id) {
        return list.stream().filter(item -> Objects.equals(InMemoryData.toLong(item.get("id")), id)).findFirst().orElse(null);
    }

    private void removeById(List<Map<String, Object>> list, Long id) {
        list.removeIf(item -> Objects.equals(InMemoryData.toLong(item.get("id")), id));
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && key.startsWith("_")) return;
            target.put(key, value);
        });
        return target;
    }

    private boolean matchKeyword(Map<String, Object> item, String keyword, String... fields) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        if (!StringUtils.hasText(key)) return true;
        for (String field : fields) {
            Object value = item.get(field);
            if (value != null && String.valueOf(value).toLowerCase().contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchUserByOrderRemark(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) return false;
        String key = keyword.trim().toLowerCase();
        return orders.stream()
                .filter(item -> Objects.equals(InMemoryData.toLong(item.get("user_id")), userId))
                .map(item -> String.valueOf(item.getOrDefault("remark", "")))
                .filter(StringUtils::hasText)
                .map(text -> text.toLowerCase())
                .anyMatch(text -> text.contains(key));
    }

    private boolean matchUserByBackpackId(Map<String, Object> user, String keyword) {
        if (user == null || !StringUtils.hasText(keyword)) return false;
        String key = keyword.trim().toLowerCase();
        String backpackId = formatBackpackId(user.get("id")).toLowerCase();
        return backpackId.contains(key);
    }

    private String normalizeUserSortField(String rawSortField) {
        String value = rawSortField == null ? "" : rawSortField.trim().toLowerCase(Locale.ROOT);
        switch (value) {
            case "id":
            case "backpack_id":
                return "id";
            case "point_balance":
                return "point_balance";
            case "total_consume_amount":
                return "total_consume_amount";
            case "profit_amount":
                return "profit_amount";
            default:
                return "";
        }
    }

    private boolean isAscSortOrder(String rawSortOrder) {
        String value = rawSortOrder == null ? "" : rawSortOrder.trim().toLowerCase(Locale.ROOT);
        return "asc".equals(value) || "ascend".equals(value) || "ascending".equals(value);
    }

    private Comparator<Map<String, Object>> buildUserSortComparator(String sortField, boolean asc) {
        Comparator<Map<String, Object>> defaultComparator = (a, b) -> {
            int createdCmp = String.valueOf(b.getOrDefault("created_at", "")).compareTo(String.valueOf(a.getOrDefault("created_at", "")));
            if (createdCmp != 0) return createdCmp;
            return Long.compare(InMemoryData.toLong(b.getOrDefault("id", 0L)), InMemoryData.toLong(a.getOrDefault("id", 0L)));
        };
        if (!StringUtils.hasText(sortField)) return defaultComparator;

        return (a, b) -> {
            int cmp;
            switch (sortField) {
                case "id":
                    cmp = Long.compare(
                            InMemoryData.toLong(a.getOrDefault("id", 0L)),
                            InMemoryData.toLong(b.getOrDefault("id", 0L))
                    );
                    break;
                case "point_balance":
                    cmp = Long.compare(
                            InMemoryData.toLong(a.getOrDefault("point_balance", 0L)),
                            InMemoryData.toLong(b.getOrDefault("point_balance", 0L))
                    );
                    break;
                case "total_consume_amount":
                    cmp = Double.compare(
                            toMoney(a.getOrDefault("total_consume_amount", 0D)),
                            toMoney(b.getOrDefault("total_consume_amount", 0D))
                    );
                    break;
                case "profit_amount":
                    cmp = Double.compare(
                            toMoney(a.getOrDefault("profit_amount", 0D)),
                            toMoney(b.getOrDefault("profit_amount", 0D))
                    );
                    break;
                default:
                    cmp = 0;
                    break;
            }
            if (!asc) cmp = -cmp;
            if (cmp != 0) return cmp;
            return defaultComparator.compare(a, b);
        };
    }

    private void mergeFields(Map<String, Object> target, Map<String, Object> payload, String... keys) {
        if (payload == null) return;
        for (String key : keys) {
            if (payload.containsKey(key)) {
                target.put(key, payload.get(key));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cloneList(List<Map<String, Object>> source) {
        synchronized (source) {
            return source.stream()
                    .map(item -> new LinkedHashMap<>((Map<String, Object>) item))
                    .collect(Collectors.toList());
        }
    }

    private void applySnapshot(Map<String, Object> snapshot) {
        replaceList(snapshot.get("users"), users);
        replaceList(snapshot.get("userAddresses"), userAddresses);
        replaceList(snapshot.get("pointLedger"), pointLedger);
        replaceList(snapshot.get("categories"), categories);
        replaceList(snapshot.get("recommendSlots"), recommendSlots);
        replaceList(snapshot.get("recommendItems"), recommendItems);
        replaceList(snapshot.get("spus"), spus);
        replaceList(snapshot.get("skus"), skus);
        replaceList(snapshot.get("medias"), medias);
        replaceList(snapshot.get("attrDefs"), attrDefs);
        replaceList(snapshot.get("attrValues"), attrValues);
        replaceList(snapshot.get("orders"), orders);
        replaceList(snapshot.get("orderItems"), orderItems);
        replaceList(snapshot.get("orderFlows"), orderFlows);
        replaceList(snapshot.get("orderDeliveries"), orderDeliveries);
        replaceList(snapshot.get("backpackAssets"), backpackAssets);
        replaceList(snapshot.get("backpackFlows"), backpackFlows);
        replaceList(snapshot.get("groupResources"), groupResources);
        replaceList(snapshot.get("dictTypes"), dictTypes);
        replaceList(snapshot.get("dictItems"), dictItems);
        replaceList(snapshot.get("systemConfigs"), systemConfigs);
        replaceList(snapshot.get("files"), files);
        replaceList(snapshot.get("adminUsers"), adminUsers);
        replaceList(snapshot.get("roles"), roles);
        replaceList(snapshot.get("permissions"), permissions);
        replaceList(snapshot.get("pointRules"), pointRules);
        replaceList(snapshot.get("wishDemands"), wishDemands);
        restoreRolePermissionMap(snapshot.get("rolePermissionMap"));
        restoreGroupResourceDeletedIds(snapshot.get("groupResourceDeletedIds"));
        importSeqSnapshot(snapshot.get("sequences"));
    }

    @SuppressWarnings("unchecked")
    private void replaceList(Object raw, List<Map<String, Object>> target) {
        if (!(raw instanceof Collection<?>)) return;
        synchronized (target) {
            target.clear();
            for (Object item : (Collection<?>) raw) {
                if (item instanceof Map<?, ?>) {
                    target.add(new LinkedHashMap<>((Map<String, Object>) item));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreRolePermissionMap(Object raw) {
        rolePermissionMap.clear();
        if (!(raw instanceof Map<?, ?>)) return;
        Map<?, ?> map = (Map<?, ?>) raw;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Long roleId = InMemoryData.toLong(entry.getKey());
            Set<Long> set = new LinkedHashSet<>();
            Object value = entry.getValue();
            if (value instanceof Collection<?>) {
                for (Object item : (Collection<?>) value) {
                    set.add(InMemoryData.toLong(item));
                }
            }
            rolePermissionMap.put(roleId, set);
        }
    }

    private void restoreGroupResourceDeletedIds(Object raw) {
        groupResourceDeletedIds.clear();
        if (!(raw instanceof Collection<?>)) return;
        for (Object item : (Collection<?>) raw) {
            groupResourceDeletedIds.add(InMemoryData.toLong(item));
        }
    }

    private Map<String, Long> exportSeqSnapshot() {
        Map<String, Long> seq = new LinkedHashMap<>();
        seq.put("pointLedgerIdSeq", pointLedgerIdSeq.get());
        seq.put("categoryIdSeq", categoryIdSeq.get());
        seq.put("slotIdSeq", slotIdSeq.get());
        seq.put("recommendItemIdSeq", recommendItemIdSeq.get());
        seq.put("spuIdSeq", spuIdSeq.get());
        seq.put("skuIdSeq", skuIdSeq.get());
        seq.put("mediaIdSeq", mediaIdSeq.get());
        seq.put("attrDefIdSeq", attrDefIdSeq.get());
        seq.put("attrValueIdSeq", attrValueIdSeq.get());
        seq.put("orderIdSeq", orderIdSeq.get());
        seq.put("orderFlowIdSeq", orderFlowIdSeq.get());
        seq.put("assetIdSeq", assetIdSeq.get());
        seq.put("assetFlowIdSeq", assetFlowIdSeq.get());
        seq.put("groupResourceIdSeq", groupResourceIdSeq.get());
        seq.put("dictTypeIdSeq", dictTypeIdSeq.get());
        seq.put("dictItemIdSeq", dictItemIdSeq.get());
        seq.put("configIdSeq", configIdSeq.get());
        seq.put("fileIdSeq", fileIdSeq.get());
        seq.put("adminUserIdSeq", adminUserIdSeq.get());
        seq.put("roleIdSeq", roleIdSeq.get());
        seq.put("permissionIdSeq", permissionIdSeq.get());
        seq.put("pointRuleIdSeq", pointRuleIdSeq.get());
        seq.put("wishDemandIdSeq", wishDemandIdSeq.get());
        return seq;
    }

    private void importSeqSnapshot(Object raw) {
        if (!(raw instanceof Map<?, ?>)) return;
        Map<?, ?> map = (Map<?, ?>) raw;
        setSeq(pointLedgerIdSeq, map.get("pointLedgerIdSeq"));
        setSeq(categoryIdSeq, map.get("categoryIdSeq"));
        setSeq(slotIdSeq, map.get("slotIdSeq"));
        setSeq(recommendItemIdSeq, map.get("recommendItemIdSeq"));
        setSeq(spuIdSeq, map.get("spuIdSeq"));
        setSeq(skuIdSeq, map.get("skuIdSeq"));
        setSeq(mediaIdSeq, map.get("mediaIdSeq"));
        setSeq(attrDefIdSeq, map.get("attrDefIdSeq"));
        setSeq(attrValueIdSeq, map.get("attrValueIdSeq"));
        setSeq(orderIdSeq, map.get("orderIdSeq"));
        setSeq(orderFlowIdSeq, map.get("orderFlowIdSeq"));
        setSeq(assetIdSeq, map.get("assetIdSeq"));
        setSeq(assetFlowIdSeq, map.get("assetFlowIdSeq"));
        setSeq(groupResourceIdSeq, map.get("groupResourceIdSeq"));
        setSeq(dictTypeIdSeq, map.get("dictTypeIdSeq"));
        setSeq(dictItemIdSeq, map.get("dictItemIdSeq"));
        setSeq(configIdSeq, map.get("configIdSeq"));
        setSeq(fileIdSeq, map.get("fileIdSeq"));
        setSeq(adminUserIdSeq, map.get("adminUserIdSeq"));
        setSeq(roleIdSeq, map.get("roleIdSeq"));
        setSeq(permissionIdSeq, map.get("permissionIdSeq"));
        setSeq(pointRuleIdSeq, map.get("pointRuleIdSeq"));
        setSeq(wishDemandIdSeq, map.get("wishDemandIdSeq"));
    }

    private void setSeq(AtomicLong seq, Object value) {
        if (value == null) return;
        seq.set(InMemoryData.toLong(value));
    }

    private void syncIdSequencesByData() {
        pointLedgerIdSeq.set(Math.max(pointLedgerIdSeq.get(), maxId(pointLedger)));
        categoryIdSeq.set(Math.max(categoryIdSeq.get(), maxId(categories)));
        slotIdSeq.set(Math.max(slotIdSeq.get(), maxId(recommendSlots)));
        recommendItemIdSeq.set(Math.max(recommendItemIdSeq.get(), maxId(recommendItems)));
        spuIdSeq.set(Math.max(spuIdSeq.get(), maxId(spus)));
        skuIdSeq.set(Math.max(skuIdSeq.get(), maxId(skus)));
        mediaIdSeq.set(Math.max(mediaIdSeq.get(), maxId(medias)));
        attrDefIdSeq.set(Math.max(attrDefIdSeq.get(), maxId(attrDefs)));
        attrValueIdSeq.set(Math.max(attrValueIdSeq.get(), maxId(attrValues)));
        orderIdSeq.set(Math.max(orderIdSeq.get(), maxId(orders)));
        orderFlowIdSeq.set(Math.max(orderFlowIdSeq.get(), maxId(orderFlows)));
        assetIdSeq.set(Math.max(assetIdSeq.get(), maxId(backpackAssets)));
        assetFlowIdSeq.set(Math.max(assetFlowIdSeq.get(), maxId(backpackFlows)));
        groupResourceIdSeq.set(Math.max(groupResourceIdSeq.get(), maxId(groupResources)));
        dictTypeIdSeq.set(Math.max(dictTypeIdSeq.get(), maxId(dictTypes)));
        dictItemIdSeq.set(Math.max(dictItemIdSeq.get(), maxId(dictItems)));
        configIdSeq.set(Math.max(configIdSeq.get(), maxId(systemConfigs)));
        fileIdSeq.set(Math.max(fileIdSeq.get(), maxId(files)));
        adminUserIdSeq.set(Math.max(adminUserIdSeq.get(), maxId(adminUsers)));
        roleIdSeq.set(Math.max(roleIdSeq.get(), maxId(roles)));
        permissionIdSeq.set(Math.max(permissionIdSeq.get(), maxId(permissions)));
        pointRuleIdSeq.set(Math.max(pointRuleIdSeq.get(), maxId(pointRules)));
        wishDemandIdSeq.set(Math.max(wishDemandIdSeq.get(), maxId(wishDemands)));
    }

    private long maxId(List<Map<String, Object>> source) {
        synchronized (source) {
            return source.stream()
                    .map(item -> InMemoryData.toLong(item.get("id")))
                    .max(Long::compareTo)
                    .orElse(0L);
        }
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> source, int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int from = (safePageNo - 1) * safePageSize;
        int to = Math.min(from + safePageSize, source.size());
        List<Map<String, Object>> list = from >= source.size()
                ? new ArrayList<>()
                : source.subList(from, to).stream().map(this::copyMap).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        result.put("total", source.size());
        result.put("list", list);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readStoredTraceList(Map<String, Object> delivery) {
        if (delivery == null) return new ArrayList<>();
        Object raw = delivery.get("trace_list");
        if (!(raw instanceof Collection<?>)) return new ArrayList<>();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : (Collection<?>) raw) {
            if (!(item instanceof Map<?, ?>)) continue;
            Map<String, Object> trace = new LinkedHashMap<>();
            Map<String, Object> source = (Map<String, Object>) item;
            String time = normalizeDateTimeText(String.valueOf(source.getOrDefault("accept_time", source.getOrDefault("AcceptTime", ""))));
            String station = String.valueOf(source.getOrDefault("accept_station", source.getOrDefault("AcceptStation", source.getOrDefault("context", ""))));
            String status = String.valueOf(source.getOrDefault("status", source.getOrDefault("Status", "")));
            String location = String.valueOf(source.getOrDefault("location", source.getOrDefault("Location", "")));
            if (!StringUtils.hasText(time) && !StringUtils.hasText(station)) continue;
            trace.put("accept_time", time);
            trace.put("accept_station", station);
            trace.put("status", status);
            trace.put("location", location);
            list.add(trace);
        }
        list.sort((a, b) -> normalizeDateTimeText(String.valueOf(b.getOrDefault("accept_time", "")))
                .compareTo(normalizeDateTimeText(String.valueOf(a.getOrDefault("accept_time", "")))));
        return list;
    }

    private List<Map<String, Object>> convertTraceNodesToList(List<KdniaoLogisticsService.TraceNode> traceNodes) {
        if (traceNodes == null || traceNodes.isEmpty()) return new ArrayList<>();
        List<Map<String, Object>> list = new ArrayList<>();
        for (KdniaoLogisticsService.TraceNode node : traceNodes) {
            if (node == null) continue;
            String time = normalizeDateTimeText(node.getAcceptTime());
            String station = String.valueOf(node.getAcceptStation());
            if (!StringUtils.hasText(time) && !StringUtils.hasText(station)) continue;
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("accept_time", time);
            trace.put("accept_station", station);
            trace.put("status", String.valueOf(node.getStatus()));
            trace.put("location", String.valueOf(node.getLocation()));
            list.add(trace);
        }
        list.sort((a, b) -> normalizeDateTimeText(String.valueOf(b.getOrDefault("accept_time", "")))
                .compareTo(normalizeDateTimeText(String.valueOf(a.getOrDefault("accept_time", "")))));
        return list;
    }

    private String now() {
        return LocalDateTime.now(BEIJING_ZONE).format(DT);
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDateTime.parse(normalizeDateTimeText(value), DT);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String normalizeDateTimeText(String value) {
        if (!StringUtils.hasText(value)) return "";
        String text = value.trim().replace('T', ' ');
        if (text.endsWith("Z")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        int dot = text.indexOf('.');
        if (dot > 0) text = text.substring(0, dot);
        return text.length() >= 19 ? text.substring(0, 19) : text;
    }

    private String firstNotBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "";
    }

    private String formatDateTime(Object value) {
        if (value == null) return now();
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime().format(DT);
        }
        if (value instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), BEIJING_ZONE).format(DT);
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) return now();
        text = text.replace('T', ' ');
        if (text.endsWith("Z")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        int dot = text.indexOf('.');
        if (dot > 0) {
            text = text.substring(0, dot);
        }
        return text.length() >= 19 ? text.substring(0, 19) : text;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Long resolveCategoryIdByName(String categoryName) {
        String name = categoryName == null ? "" : categoryName.trim();
        if (!StringUtils.hasText(name)) return null;
        Map<String, Object> target = categories.stream()
                .filter(item -> name.equals(String.valueOf(item.getOrDefault("category_name", "")).trim()))
                .findFirst()
                .orElse(null);
        return target == null ? null : InMemoryData.toLong(target.get("id"));
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveCategoryNames(Map<String, Object> payload, String fallbackCategoryName) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (payload != null) {
            Object rawList = payload.get("category_names");
            if (rawList instanceof Collection<?>) {
                for (Object one : (Collection<Object>) rawList) {
                    String text = String.valueOf(one == null ? "" : one).trim();
                    if (StringUtils.hasText(text)) names.add(text);
                }
            } else if (rawList instanceof String) {
                String[] split = String.valueOf(rawList).split("[,，/]+");
                for (String one : split) {
                    String text = String.valueOf(one == null ? "" : one).trim();
                    if (StringUtils.hasText(text)) names.add(text);
                }
            }
            if (names.isEmpty()) {
                String single = String.valueOf(payload.getOrDefault("category_name", "")).trim();
                if (StringUtils.hasText(single)) names.add(single);
            }
        }
        if (names.isEmpty() && StringUtils.hasText(fallbackCategoryName)) {
            String[] split = fallbackCategoryName.split("[,，/]+");
            for (String one : split) {
                String text = String.valueOf(one == null ? "" : one).trim();
                if (StringUtils.hasText(text)) names.add(text);
            }
        }
        if (names.isEmpty()) {
            names.add("未分类");
        }
        return new ArrayList<>(names);
    }

    private List<Long> resolveCategoryIdsByNames(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) return new ArrayList<>();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String name : categoryNames) {
            Long categoryId = resolveCategoryIdByName(name);
            if (categoryId != null && categoryId > 0) {
                ids.add(categoryId);
            }
        }
        return new ArrayList<>(ids);
    }

    private boolean matchesCategoryIdFromProduct(Map<String, Object> product, Long categoryId) {
        if (product == null || categoryId == null) return false;
        Long primary = InMemoryData.toLong(product.getOrDefault("category_id", 0));
        if (Objects.equals(primary, categoryId)) return true;
        Object raw = product.get("category_ids");
        if (raw instanceof Collection<?>) {
            for (Object item : (Collection<?>) raw) {
                if (Objects.equals(InMemoryData.toLong(item), categoryId)) return true;
            }
        }
        return false;
    }

    private List<Long> extractCategoryIdsFromProduct(Map<String, Object> product) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (product == null) return new ArrayList<>();
        Object raw = product.get("category_ids");
        if (raw instanceof Collection<?>) {
            for (Object item : (Collection<?>) raw) {
                Long id = InMemoryData.toLong(item);
                if (id != null && id > 0) ids.add(id);
            }
        } else if (raw instanceof String) {
            String[] split = String.valueOf(raw).split("[,，\\s]+");
            for (String part : split) {
                if (!StringUtils.hasText(part)) continue;
                try {
                    Long id = Long.parseLong(part.trim());
                    if (id > 0) ids.add(id);
                } catch (Exception ignore) {
                    // ignore invalid token
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private Map<String, Object> user(Long id, String nickName, String phone, String status, long pointBalance, int orderCount, String createdAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("nick_name", nickName);
        item.put("phone_masked", maskPhone(phone));
        item.put("phone", phone);
        item.put("user_status_code", status);
        item.put("point_balance", pointBalance);
        item.put("order_count", orderCount);
        item.put("admin_remark", "");
        item.put("total_consume_amount", 0D);
        item.put("profit_amount", 0D);
        item.put("created_at", createdAt);
        return item;
    }

    private Map<String, Object> address(Long id, Long userId, String receiverName, String receiverPhone, String detailAddress) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("user_id", userId);
        item.put("receiver_name", receiverName);
        item.put("receiver_phone", receiverPhone);
        item.put("detail_address", detailAddress);
        item.put("province_name", "");
        item.put("city_name", "");
        item.put("district_name", "");
        return item;
    }

    private Map<String, Object> ledger(Long id, Long userId, String userName, String bizTypeCode, long changeAmount, long balanceAfter, String occurredAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("user_id", userId);
        item.put("user_name", userName);
        item.put("biz_type_code", bizTypeCode);
        item.put("change_amount", changeAmount);
        item.put("balance_after", balanceAfter);
        item.put("note", "");
        item.put("occurred_at", occurredAt);
        return item;
    }

    private Map<String, Object> category(Long id, String name, int sortNo, String statusCode, int productCount, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("category_name", name);
        item.put("sort_no", sortNo);
        item.put("status_code", statusCode);
        item.put("product_count", productCount);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> slot(Long id, String slotName, String slotCode, String statusCode, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("slot_name", slotName);
        item.put("slot_code", slotCode);
        item.put("status_code", statusCode);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> recommendItem(Long id, Long slotId, Long spuId, String productName, long pointPrice,
                                              int sortNo, String statusCode, String startAt, String endAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("slot_id", slotId);
        item.put("spu_id", spuId);
        item.put("product_name", productName);
        item.put("point_price", pointPrice);
        item.put("sort_no", sortNo);
        item.put("status_code", statusCode);
        item.put("start_at", startAt);
        item.put("end_at", endAt);
        return item;
    }

    private Map<String, Object> spu(Long id, String spuName, String categoryName, long minPrice, long maxPrice, long totalStock,
                                    String statusCode, boolean recommendFlag, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("skc_code", buildSkcCode(id == null ? 0L : id));
        item.put("spu_name", spuName);
        item.put("category_name", categoryName);
        item.put("point_price_min", minPrice);
        item.put("point_price_max", maxPrice);
        item.put("total_stock", totalStock);
        item.put("status_code", statusCode);
        item.put("recommend_flag", recommendFlag);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> sku(Long id, Long spuId, String skuName, String specText, long pointPrice, long stock, String statusCode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("sku_code", buildSkuCode(id == null ? 0L : id));
        item.put("spu_id", spuId);
        item.put("sku_name", skuName);
        item.put("spec_text", specText);
        item.put("point_price", pointPrice);
        item.put("stock_available", stock);
        item.put("status_code", statusCode);
        return item;
    }

    private Map<String, Object> media(Long id, Long spuId, String mediaType, String mediaUrl, int sortNo) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("spu_id", spuId);
        item.put("media_type", mediaType);
        item.put("media_url", mediaUrl);
        item.put("sort_no", sortNo);
        return item;
    }

    private Map<String, Object> attrDef(Long id, String attrName, String attrCode, String valueType, boolean requiredFlag,
                                        String statusCode, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("attr_name", attrName);
        item.put("attr_code", attrCode);
        item.put("value_type", valueType);
        item.put("required_flag", requiredFlag);
        item.put("status_code", statusCode);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> order(Long id, Long userId, String orderNo, String userName, String orderStatusCode,
                                      long totalPointAmount, String submitAt, String remark) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("user_id", userId);
        item.put("order_no", orderNo);
        item.put("user_name", userName);
        item.put("order_status_code", orderStatusCode);
        item.put("total_point_amount", totalPointAmount);
        item.put("submit_at", submitAt);
        item.put("remark", remark);
        item.put("user_remark", remark);
        item.put("procurement_status", "PENDING_PROCURE");
        item.put("procurement_status_text", "待采购");
        item.put("procured_at", "");
        item.put("procured_by", "");
        return item;
    }

    private Map<String, Object> orderItem(Long id, Long orderId, String spuName, String skuName, int quantity, long pointPrice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("order_id", orderId);
        item.put("spu_name", spuName);
        item.put("sku_name", skuName);
        item.put("quantity", quantity);
        item.put("point_price", pointPrice);
        item.put("total_point_amount", pointPrice * quantity);
        return item;
    }

    private Map<String, Object> orderFlow(Long id, Long orderId, String fromStatus, String toStatus,
                                          String actionText, String note, String operatorName, String occurredAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("order_id", orderId);
        item.put("from_status", fromStatus);
        item.put("to_status", toStatus);
        item.put("action_text", actionText);
        item.put("note", note);
        item.put("operator_name", operatorName);
        item.put("occurred_at", occurredAt);
        return item;
    }

    private Map<String, Object> delivery(Long orderId, String receiverName, String receiverPhone, String receiverAddress,
                                         String expressCompany, String expressNo, String shipAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("order_id", orderId);
        item.put("receiver_name", receiverName);
        item.put("receiver_phone", receiverPhone);
        item.put("province_name", "");
        item.put("city_name", "");
        item.put("district_name", "");
        item.put("receiver_address", receiverAddress);
        item.put("express_company", expressCompany);
        item.put("express_no", expressNo);
        item.put("ship_at", shipAt);
        return item;
    }

    private Map<String, Object> asset(Long id, String assetNo, String userName, String assetName,
                                      String assetTypeCode, String statusCode, String obtainedAt, String expireAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("asset_no", assetNo);
        item.put("user_name", userName);
        item.put("asset_name", assetName);
        item.put("asset_type_code", assetTypeCode);
        item.put("status_code", statusCode);
        item.put("obtained_at", obtainedAt);
        item.put("expire_at", expireAt);
        return item;
    }

    private Map<String, Object> assetFlow(Long id, Long assetId, String actionTypeCode, String actionText,
                                          String note, String operatorName, String occurredAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("asset_id", assetId);
        item.put("action_type_code", actionTypeCode);
        item.put("action_text", actionText);
        item.put("note", note);
        item.put("operator_name", operatorName);
        item.put("occurred_at", occurredAt);
        return item;
    }

    private Map<String, Object> group(Long id, String groupName, String qrImageUrl, String introText,
                                      int maxMemberCount, int currentMemberCount, String expireAt,
                                      String statusCode, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("group_name", groupName);
        item.put("qr_image_url", qrImageUrl);
        item.put("intro_text", introText);
        item.put("max_member_count", maxMemberCount);
        item.put("current_member_count", currentMemberCount);
        item.put("expire_at", expireAt);
        item.put("status_code", statusCode);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> wishDemand(Long id, Long userId, String userName, String phoneMasked,
                                           String wishTitle, String wishMessage, String imageUrl,
                                           String statusCode, String decisionNote, String decidedAt, String decidedBy) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("user_id", userId);
        item.put("user_name", userName);
        item.put("phone_masked", phoneMasked);
        item.put("wish_title", wishTitle);
        item.put("wish_message", wishMessage);
        item.put("image_url", imageUrl);
        item.put("image_urls", StringUtils.hasText(imageUrl) ? new ArrayList<>(List.of(imageUrl)) : new ArrayList<>());
        item.put("status_code", statusCode);
        item.put("status_text", wishDemandStatusText(statusCode));
        item.put("decision_note", decisionNote);
        item.put("notify_content", "");
        item.put("created_at", now());
        item.put("updated_at", now());
        item.put("decided_at", decidedAt);
        item.put("decided_by", decidedBy);
        return item;
    }

    private Map<String, Object> dictType(Long id, String code, String name, String statusCode, String remark, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("dict_type_code", code);
        item.put("dict_type_name", name);
        item.put("status_code", statusCode);
        item.put("remark", remark);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> dictItem(Long id, String typeCode, String itemCode, String itemName,
                                         String itemValue, int sortNo, String statusCode, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("dict_type_code", typeCode);
        item.put("item_code", itemCode);
        item.put("item_name", itemName);
        item.put("item_value", itemValue);
        item.put("sort_no", sortNo);
        item.put("status_code", statusCode);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> config(Long id, String configKey, String configName, String configValue,
                                       String valueTypeCode, String groupCode, String statusCode,
                                       String remark, String updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("config_key", configKey);
        item.put("config_name", configName);
        item.put("config_value", configValue);
        item.put("value_type_code", valueTypeCode);
        item.put("group_code", groupCode);
        item.put("status_code", statusCode);
        item.put("remark", remark);
        item.put("updated_at", updatedAt);
        return item;
    }

    private Map<String, Object> file(Long id, String fileName, String fileUrl, String mimeType, long fileSizeKb, String uploadedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("file_name", fileName);
        item.put("file_url", fileUrl);
        item.put("mime_type", mimeType);
        item.put("file_size_kb", fileSizeKb);
        item.put("uploaded_at", uploadedAt);
        return item;
    }

    private Map<String, Object> adminUser(Long id, String username, String displayName, String phone,
                                          String statusCode, List<String> roles, String password, String lastLoginAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("username", username);
        item.put("display_name", displayName);
        item.put("phone", phone);
        item.put("status_code", statusCode);
        item.put("roles", new ArrayList<>(roles));
        item.put("_password", password);
        item.put("last_login_at", lastLoginAt);
        return item;
    }

    private Map<String, Object> role(Long id, String roleName, String roleCode, String statusCode,
                                     int permissionCount, String remark) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("role_name", roleName);
        item.put("role_code", roleCode);
        item.put("status_code", statusCode);
        item.put("permission_count", permissionCount);
        item.put("remark", remark);
        return item;
    }

    private Map<String, Object> permission(Long id, String permissionName, String permissionCode,
                                           String moduleName, String method, String path, Long parentId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("permission_name", permissionName);
        item.put("permission_code", permissionCode);
        item.put("module_name", moduleName);
        item.put("method", method);
        item.put("path", path);
        item.put("parent_id", parentId);
        return item;
    }

    private Map<String, Object> pointRule(Long id, String ruleName, String ruleCode, long changeValue, String statusCode, String remark) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("rule_name", ruleName);
        item.put("rule_code", ruleCode);
        item.put("change_value", changeValue);
        item.put("status_code", statusCode);
        item.put("remark", remark);
        item.put("updated_at", now());
        return item;
    }

    private List<Map<String, Object>> buildPermissionTree() {
        List<Map<String, Object>> roots = permissions.stream()
                .filter(item -> item.get("parent_id") == null)
                .sorted(Comparator.comparingLong(item -> InMemoryData.toLong(item.get("id"))))
                .collect(Collectors.toList());

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> root : roots) {
            Long rootId = InMemoryData.toLong(root.get("id"));
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("label", root.get("permission_name") + "（" + root.get("permission_code") + "）");
            node.put("key", rootId);

            List<Map<String, Object>> children = permissions.stream()
                    .filter(item -> Objects.equals(item.get("parent_id"), rootId))
                    .sorted(Comparator.comparingLong(item -> InMemoryData.toLong(item.get("id"))))
                    .map(item -> {
                        Map<String, Object> child = new LinkedHashMap<>();
                        child.put("label", item.get("permission_name") + "（" + item.get("permission_code") + "）");
                        child.put("key", InMemoryData.toLong(item.get("id")));
                        return child;
                    })
                    .collect(Collectors.toList());
            if (!children.isEmpty()) {
                node.put("children", children);
            }
            tree.add(node);
        }
        return tree;
    }
}
