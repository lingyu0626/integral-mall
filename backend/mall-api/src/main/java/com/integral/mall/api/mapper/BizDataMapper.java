package com.integral.mall.api.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BizDataMapper {

    private static final Logger log = LoggerFactory.getLogger(BizDataMapper.class);

    private final JdbcTemplate jdbcTemplate;

    public BizDataMapper(org.springframework.beans.factory.ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
    }

    public boolean available() {
        return jdbcTemplate != null;
    }

    @PostConstruct
    public void initTables() {
        if (jdbcTemplate == null) return;
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_category (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "category_no VARCHAR(32) NOT NULL DEFAULT ''," +
                "parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "level_no INT NOT NULL DEFAULT 1," +
                "category_name VARCHAR(128) NOT NULL," +
                "sort_no INT NOT NULL DEFAULT 0," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ENABLED'," +
                "leaf_flag TINYINT(1) NOT NULL DEFAULT 1," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_category_no (tenant_id, category_no)," +
                "KEY idx_category_status_sort (status_code, sort_no)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_product_spu (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "product_no VARCHAR(32) NOT NULL DEFAULT ''," +
                "product_name VARCHAR(256) NOT NULL," +
                "product_type_code VARCHAR(32) NOT NULL DEFAULT 'PHYSICAL'," +
                "category_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "unit_name VARCHAR(32) NOT NULL DEFAULT '件'," +
                "main_image_url VARCHAR(512) NULL," +
                "detail_html LONGTEXT NULL," +
                "point_price BIGINT NOT NULL DEFAULT 0," +
                "stock_total INT NOT NULL DEFAULT 0," +
                "stock_available INT NOT NULL DEFAULT 0," +
                "sale_status_code VARCHAR(32) NOT NULL DEFAULT 'OFF_SHELF'," +
                "recommend_flag TINYINT(1) NOT NULL DEFAULT 0," +
                "sort_no INT NOT NULL DEFAULT 0," +
                "limit_per_user INT NOT NULL DEFAULT 0," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_spu_no (tenant_id, product_no)," +
                "KEY idx_spu_category (category_id)," +
                "KEY idx_spu_sale_status (sale_status_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        ensureColumnExists("pm_product_spu", "detail_html", "LONGTEXT NULL");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_product_sku (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "spu_id BIGINT UNSIGNED NOT NULL," +
                "sku_no VARCHAR(32) NOT NULL DEFAULT ''," +
                "sku_name VARCHAR(256) NOT NULL," +
                "point_price BIGINT NOT NULL DEFAULT 0," +
                "stock_total INT NOT NULL DEFAULT 0," +
                "stock_available INT NOT NULL DEFAULT 0," +
                "sale_status_code VARCHAR(32) NOT NULL DEFAULT 'OFF_SHELF'," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_sku_no (tenant_id, sku_no)," +
                "KEY idx_sku_spu (spu_id)," +
                "KEY idx_sku_status (sale_status_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_product_media (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "spu_id BIGINT UNSIGNED NOT NULL," +
                "sku_id BIGINT UNSIGNED NULL," +
                "media_type_code VARCHAR(32) NOT NULL," +
                "media_url VARCHAR(512) NOT NULL," +
                "media_name VARCHAR(128) NULL," +
                "sort_no INT NOT NULL DEFAULT 0," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ENABLED'," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "KEY idx_media_spu (spu_id)," +
                "KEY idx_media_type_status (media_type_code, status_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_user (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "app_id VARCHAR(64) NOT NULL DEFAULT 'wxapp'," +
                "user_no VARCHAR(32) NOT NULL," +
                "union_id VARCHAR(128) NULL," +
                "open_id VARCHAR(128) NULL," +
                "phone VARCHAR(20) NULL," +
                "phone_masked VARCHAR(20) NULL," +
                "nick_name VARCHAR(128) NULL," +
                "avatar_url VARCHAR(512) NULL," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                "register_channel_code VARCHAR(64) NOT NULL DEFAULT 'WXAPP'," +
                "register_at DATETIME(3) NULL," +
                "remark VARCHAR(500) NULL," +
                "ext_json JSON NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_user_tenant_user_no (tenant_id, user_no)," +
                "UNIQUE KEY uk_user_tenant_open_id (tenant_id, open_id)," +
                "KEY idx_user_phone (phone)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_point_account (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "user_id BIGINT UNSIGNED NOT NULL," +
                "point_balance BIGINT NOT NULL DEFAULT 0," +
                "point_frozen BIGINT NOT NULL DEFAULT 0," +
                "point_total_income BIGINT NOT NULL DEFAULT 0," +
                "point_total_expense BIGINT NOT NULL DEFAULT 0," +
                "account_status_code VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                "last_change_at DATETIME(3) NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_point_account_user (tenant_id, user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_user_address (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "user_id BIGINT UNSIGNED NOT NULL," +
                "receiver_name VARCHAR(64) NOT NULL," +
                "receiver_phone VARCHAR(20) NOT NULL," +
                "province_name VARCHAR(64) NOT NULL DEFAULT ''," +
                "city_name VARCHAR(64) NOT NULL DEFAULT ''," +
                "district_name VARCHAR(64) NOT NULL DEFAULT ''," +
                "detail_address VARCHAR(255) NOT NULL," +
                "is_default TINYINT(1) NOT NULL DEFAULT 0," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "KEY idx_addr_user (user_id)," +
                "KEY idx_addr_user_default (user_id, is_default)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_exchange_order (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "app_id VARCHAR(64) NOT NULL DEFAULT 'wxapp'," +
                "order_no VARCHAR(32) NOT NULL," +
                "user_id BIGINT UNSIGNED NOT NULL," +
                "order_type_code VARCHAR(32) NOT NULL DEFAULT 'EXCHANGE'," +
                "product_type_code VARCHAR(32) NOT NULL DEFAULT 'PHYSICAL'," +
                "order_status_code VARCHAR(32) NOT NULL," +
                "pay_status_code VARCHAR(32) NOT NULL DEFAULT 'NOT_APPLICABLE'," +
                "delivery_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING'," +
                "total_point_amount BIGINT NOT NULL," +
                "total_item_count INT NOT NULL DEFAULT 1," +
                "user_remark VARCHAR(500) NULL," +
                "admin_remark VARCHAR(500) NULL," +
                "reject_reason VARCHAR(500) NULL," +
                "idempotent_no VARCHAR(64) NULL," +
                "request_no VARCHAR(64) NULL," +
                "submit_at DATETIME(3) NOT NULL," +
                "audit_at DATETIME(3) NULL," +
                "ship_at DATETIME(3) NULL," +
                "finish_at DATETIME(3) NULL," +
                "cancel_at DATETIME(3) NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_order_no (tenant_id, order_no)," +
                "KEY idx_order_user (user_id)," +
                "KEY idx_order_status (order_status_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_exchange_order_item (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "order_id BIGINT UNSIGNED NOT NULL," +
                "order_no VARCHAR(32) NOT NULL," +
                "user_id BIGINT UNSIGNED NOT NULL," +
                "spu_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "sku_id BIGINT UNSIGNED NULL," +
                "product_no VARCHAR(32) NOT NULL DEFAULT ''," +
                "product_name_snapshot VARCHAR(256) NOT NULL," +
                "product_type_code VARCHAR(32) NOT NULL DEFAULT 'PHYSICAL'," +
                "main_image_snapshot VARCHAR(512) NULL," +
                "unit_point_price BIGINT NOT NULL," +
                "quantity INT NOT NULL DEFAULT 1," +
                "total_point_amount BIGINT NOT NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "KEY idx_item_order (order_id)," +
                "KEY idx_item_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_order_flow (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "order_id BIGINT UNSIGNED NOT NULL," +
                "order_no VARCHAR(32) NOT NULL," +
                "from_status_code VARCHAR(32) NULL," +
                "to_status_code VARCHAR(32) NOT NULL," +
                "action_code VARCHAR(64) NOT NULL," +
                "action_reason VARCHAR(500) NULL," +
                "operator_type_code VARCHAR(32) NOT NULL DEFAULT 'ADMIN'," +
                "operator_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "operated_at DATETIME(3) NOT NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "KEY idx_flow_order (order_id)," +
                "KEY idx_flow_operated (operated_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_order_delivery (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "order_id BIGINT UNSIGNED NOT NULL," +
                "order_no VARCHAR(32) NOT NULL," +
                "delivery_type_code VARCHAR(32) NOT NULL DEFAULT 'EXPRESS'," +
                "logistics_company VARCHAR(128) NULL," +
                "logistics_no VARCHAR(128) NULL," +
                "shipper_phone VARCHAR(20) NULL," +
                "shipped_at DATETIME(3) NULL," +
                "signed_at DATETIME(3) NULL," +
                "delivery_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING'," +
                "remark VARCHAR(500) NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_delivery_order (tenant_id, order_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_point_ledger (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "ledger_no VARCHAR(32) NOT NULL," +
                "user_id BIGINT UNSIGNED NOT NULL," +
                "account_id BIGINT UNSIGNED NOT NULL," +
                "change_type_code VARCHAR(32) NOT NULL," +
                "business_type_code VARCHAR(32) NOT NULL," +
                "source_type_code VARCHAR(32) NOT NULL DEFAULT 'SYSTEM'," +
                "direction_code VARCHAR(16) NOT NULL," +
                "change_amount BIGINT NOT NULL," +
                "before_balance BIGINT NOT NULL," +
                "after_balance BIGINT NOT NULL," +
                "occurred_at DATETIME(3) NOT NULL," +
                "channel_code VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'," +
                "operator_type_code VARCHAR(32) NOT NULL DEFAULT 'ADMIN'," +
                "operator_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "remark VARCHAR(500) NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_ledger_no (ledger_no)," +
                "KEY idx_ledger_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_system_config (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "config_key VARCHAR(128) NOT NULL," +
                "config_name VARCHAR(128) NOT NULL," +
                "config_value TEXT NULL," +
                "value_type_code VARCHAR(32) NOT NULL DEFAULT 'STRING'," +
                "group_code VARCHAR(64) NOT NULL DEFAULT 'DEFAULT'," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ENABLED'," +
                "remark VARCHAR(500) NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_cfg_key (tenant_id, config_key)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_recommend_slot (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "slot_code VARCHAR(64) NOT NULL," +
                "slot_name VARCHAR(128) NOT NULL," +
                "page_code VARCHAR(64) NOT NULL DEFAULT 'HOME'," +
                "scene_code VARCHAR(64) NULL," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ENABLED'," +
                "sort_no INT NOT NULL DEFAULT 0," +
                "remark VARCHAR(500) NULL," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "UNIQUE KEY uk_slot_code (tenant_id, slot_code)," +
                "KEY idx_slot_page (page_code, status_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pm_recommend_item (" +
                "id BIGINT UNSIGNED PRIMARY KEY," +
                "tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "slot_id BIGINT UNSIGNED NOT NULL," +
                "target_type_code VARCHAR(32) NOT NULL," +
                "target_id BIGINT UNSIGNED NOT NULL," +
                "title VARCHAR(256) NULL," +
                "sub_title VARCHAR(256) NULL," +
                "image_url VARCHAR(512) NULL," +
                "sort_no INT NOT NULL DEFAULT 0," +
                "valid_start_at DATETIME(3) NULL," +
                "valid_end_at DATETIME(3) NULL," +
                "status_code VARCHAR(32) NOT NULL DEFAULT 'ENABLED'," +
                "created_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)," +
                "updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0," +
                "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "deleted_at DATETIME(3) NULL," +
                "KEY idx_rec_item_slot (slot_id)," +
                "KEY idx_rec_item_target (target_type_code, target_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (Exception e) {
            log.warn("BizDataMapper 初始化失败，已降级为非持久化模式: {}", e.getMessage());
        }
    }

    public void syncCoreBusinessData(List<Map<String, Object>> categories,
                                     List<Map<String, Object>> spus,
                                     List<Map<String, Object>> skus,
                                     List<Map<String, Object>> medias,
                                     List<Map<String, Object>> users,
                                     List<Map<String, Object>> userAddresses,
                                     List<Map<String, Object>> pointLedger,
                                     List<Map<String, Object>> orders,
                                     List<Map<String, Object>> orderItems,
                                     List<Map<String, Object>> orderFlows,
                                     List<Map<String, Object>> orderDeliveries,
                                     List<Map<String, Object>> recommendSlots,
                                     List<Map<String, Object>> recommendItems,
                                     List<Map<String, Object>> systemConfigs) {
        if (!available()) return;
        safeSync("pm_category", () -> syncCategories(categories));
        safeSync("pm_product_spu", () -> syncSpus(spus));
        safeSync("pm_product_sku", () -> syncSkus(skus));
        safeSync("pm_product_media", () -> syncMedias(medias));
        safeSync("pm_user", () -> syncUsers(users));
        safeSync("pm_point_account", () -> syncPointAccounts(users));
        safeSync("pm_user_address", () -> syncUserAddresses(userAddresses));
        safeSync("pm_point_ledger", () -> syncPointLedgers(pointLedger));

        Map<Long, String> orderNoMap = new LinkedHashMap<>();
        safeSync("pm_exchange_order", () -> orderNoMap.putAll(syncOrders(orders)));
        safeSync("pm_exchange_order_item", () -> syncOrderItems(orderItems, orderNoMap, orders));
        safeSync("pm_order_flow", () -> syncOrderFlows(orderFlows, orderNoMap));
        safeSync("pm_order_delivery", () -> syncOrderDeliveries(orderDeliveries, orderNoMap));
        safeSync("pm_recommend_slot", () -> syncRecommendSlots(recommendSlots));
        safeSync("pm_recommend_item", () -> syncRecommendItems(recommendItems));
        safeSync("pm_system_config", () -> syncSystemConfigs(systemConfigs));
    }

    private void safeSync(String tag, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("同步 {} 失败: {}", tag, e.getMessage());
        }
    }

    private void syncCategories(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_category " +
                "(id, tenant_id, category_no, parent_id, level_no, category_name, sort_no, status_code, leaf_flag, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,0,1,?,?,?,1,0,0,0) " +
                "ON DUPLICATE KEY UPDATE category_name=VALUES(category_name), sort_no=VALUES(sort_no), status_code=VALUES(status_code), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> new Object[]{
                toLong(row.get("id")),
                "CAT" + String.format("%06d", Math.max(0L, toLong(row.get("id")))),
                text(row.get("category_name"), "未命名分类"),
                toInt(row.get("sort_no"), 0),
                normalizeEnabledStatus(text(row.get("status_code"), "ENABLED"))
        });
        markDeletedByIds("pm_category", collectIds(rows));
    }

    private void syncSpus(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_product_spu " +
                "(id, tenant_id, product_no, product_name, product_type_code, category_id, unit_name, main_image_url, detail_html, point_price, stock_total, stock_available, sale_status_code, recommend_flag, sort_no, limit_per_user, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE product_name=VALUES(product_name), product_type_code=VALUES(product_type_code), category_id=VALUES(category_id), main_image_url=VALUES(main_image_url), detail_html=VALUES(detail_html), point_price=VALUES(point_price), stock_total=VALUES(stock_total), stock_available=VALUES(stock_available), sale_status_code=VALUES(sale_status_code), recommend_flag=VALUES(recommend_flag), sort_no=VALUES(sort_no), limit_per_user=VALUES(limit_per_user), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long spuId = toLong(row.get("id"));
            long stock = toLong(row.getOrDefault("stock_available", row.getOrDefault("total_stock", 0)));
            return new Object[]{
                    spuId,
                    "SPU" + String.format("%06d", Math.max(0L, spuId)),
                    text(row.getOrDefault("spu_name", row.get("product_name")), "未命名商品"),
                    text(row.getOrDefault("product_type_code", "PHYSICAL"), "PHYSICAL"),
                    resolveCategoryId(row),
                    text(row.getOrDefault("unit_name", "件"), "件"),
                    text(row.getOrDefault("main_image_url", ""), ""),
                    text(row.getOrDefault("detail_html", ""), ""),
                    toLong(row.getOrDefault("point_price", row.getOrDefault("point_price_min", 0))),
                    (int) Math.max(0L, stock),
                    (int) Math.max(0L, stock),
                    normalizeSaleStatus(text(row.getOrDefault("status_code", row.getOrDefault("sale_status_code", "OFF_SHELF")), "OFF_SHELF")),
                    toBool(row.get("recommend_flag")) ? 1 : 0,
                    toInt(row.getOrDefault("sort_no", 0), 0),
                    Math.max(0, toInt(row.getOrDefault("limit_per_user", 0), 0))
            };
        });
        markDeletedByIds("pm_product_spu", collectIds(rows));
    }

    private void syncSkus(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_product_sku " +
                "(id, tenant_id, spu_id, sku_no, sku_name, point_price, stock_total, stock_available, sale_status_code, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE spu_id=VALUES(spu_id), sku_name=VALUES(sku_name), point_price=VALUES(point_price), stock_total=VALUES(stock_total), stock_available=VALUES(stock_available), sale_status_code=VALUES(sale_status_code), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long skuId = toLong(row.get("id"));
            long stock = toLong(row.getOrDefault("stock_available", 0));
            return new Object[]{
                    skuId,
                    toLong(row.get("spu_id")),
                    "SKU" + String.format("%06d", Math.max(0L, skuId)),
                    text(row.get("sku_name"), "默认规格"),
                    toLong(row.get("point_price")),
                    (int) Math.max(0L, stock),
                    (int) Math.max(0L, stock),
                    normalizeSaleStatus(text(row.getOrDefault("status_code", "OFF_SHELF"), "OFF_SHELF"))
            };
        });
        markDeletedByIds("pm_product_sku", collectIds(rows));
    }

    private void syncMedias(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_product_media " +
                "(id, tenant_id, spu_id, media_type_code, media_url, media_name, sort_no, status_code, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE spu_id=VALUES(spu_id), media_type_code=VALUES(media_type_code), media_url=VALUES(media_url), media_name=VALUES(media_name), sort_no=VALUES(sort_no), status_code=VALUES(status_code), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> new Object[]{
                toLong(row.get("id")),
                toLong(row.get("spu_id")),
                text(row.getOrDefault("media_type", row.get("media_type_code")), "IMAGE"),
                text(row.get("media_url"), ""),
                text(row.getOrDefault("media_name", ""), ""),
                toInt(row.get("sort_no"), 0),
                normalizeEnabledStatus(text(row.getOrDefault("status_code", "ENABLED"), "ENABLED"))
        });
        markDeletedByIds("pm_product_media", collectIds(rows));
    }

    private void syncUsers(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_user " +
                "(id, tenant_id, app_id, user_no, union_id, open_id, phone, phone_masked, nick_name, avatar_url, status_code, register_channel_code, register_at, created_by, updated_by, is_deleted, ext_json, remark) " +
                "VALUES (?,0,'wxapp',?,?,?,?,?,?,?,?,'WXAPP',NOW(3),0,0,0,?,?) " +
                "ON DUPLICATE KEY UPDATE union_id=VALUES(union_id), open_id=VALUES(open_id), phone=VALUES(phone), phone_masked=VALUES(phone_masked), nick_name=VALUES(nick_name), avatar_url=VALUES(avatar_url), status_code=VALUES(status_code), is_deleted=0, deleted_at=NULL, ext_json=VALUES(ext_json), remark=VALUES(remark), updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long userId = toLong(row.get("id"));
            String extJson = "{\"total_consume_amount\":" + toMoney(row.getOrDefault("total_consume_amount", 0D)) +
                    ",\"profit_amount\":" + toMoney(row.getOrDefault("profit_amount", 0D)) + "}";
            return new Object[]{
                    userId,
                    text(row.getOrDefault("user_no", "U" + userId), "U" + userId),
                    nullIfBlank(text(row.get("union_id"), "")),
                    nullIfBlank(text(row.get("open_id"), "")),
                    text(row.get("phone"), ""),
                    text(row.get("phone_masked"), ""),
                    text(row.get("nick_name"), "微信用户" + userId),
                    text(row.get("avatar_url"), ""),
                    normalizeUserStatus(text(row.getOrDefault("user_status_code", row.get("status_code")), "ACTIVE")),
                    extJson,
                    text(row.getOrDefault("admin_remark", ""), "")
            };
        });
        markDeletedByIds("pm_user", collectIds(rows));
    }

    private void syncPointAccounts(List<Map<String, Object>> users) {
        final String upsertSql = "INSERT INTO pm_point_account " +
                "(id, tenant_id, user_id, point_balance, point_frozen, point_total_income, point_total_expense, account_status_code, last_change_at, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,0,0,0,'ACTIVE',NOW(3),0,0,0) " +
                "ON DUPLICATE KEY UPDATE point_balance=VALUES(point_balance), account_status_code=VALUES(account_status_code), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(users, upsertSql, row -> {
            long userId = toLong(row.get("id"));
            return new Object[]{
                    userId,
                    userId,
                    toLong(row.getOrDefault("point_balance", 0))
            };
        });
        markDeletedByIds("pm_point_account", collectIds(users));
    }

    private void syncUserAddresses(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_user_address " +
                "(id, tenant_id, user_id, receiver_name, receiver_phone, province_name, city_name, district_name, detail_address, is_default, status_code, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), receiver_name=VALUES(receiver_name), receiver_phone=VALUES(receiver_phone), province_name=VALUES(province_name), city_name=VALUES(city_name), district_name=VALUES(district_name), detail_address=VALUES(detail_address), is_default=VALUES(is_default), status_code=VALUES(status_code), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> new Object[]{
                toLong(row.get("id")),
                toLong(row.get("user_id")),
                text(row.get("receiver_name"), ""),
                text(row.get("receiver_phone"), ""),
                text(row.get("province_name"), ""),
                text(row.get("city_name"), ""),
                text(row.get("district_name"), ""),
                text(row.get("detail_address"), ""),
                toBool(row.get("default_flag")) || toBool(row.get("is_default")) ? 1 : 0,
                normalizeUserStatus(text(row.getOrDefault("status_code", "ACTIVE"), "ACTIVE"))
        });
        markDeletedByIds("pm_user_address", collectIds(rows));
    }

    private void syncPointLedgers(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_point_ledger " +
                "(id, tenant_id, ledger_no, user_id, account_id, change_type_code, business_type_code, source_type_code, direction_code, change_amount, before_balance, after_balance, occurred_at, channel_code, operator_type_code, operator_id, remark, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,?,?,?,'ADMIN',0,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), account_id=VALUES(account_id), change_type_code=VALUES(change_type_code), business_type_code=VALUES(business_type_code), direction_code=VALUES(direction_code), change_amount=VALUES(change_amount), before_balance=VALUES(before_balance), after_balance=VALUES(after_balance), occurred_at=VALUES(occurred_at), remark=VALUES(remark), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long id = toLong(row.get("id"));
            long userId = toLong(row.get("user_id"));
            long change = toLong(row.getOrDefault("change_amount", 0));
            long after = toLong(row.getOrDefault("balance_after", 0));
            long before = after - change;
            if (before < 0) before = 0;
            String direction = change >= 0 ? "IN" : "OUT";
            String changeType = change >= 0 ? "INCOME" : "EXPENSE";
            String occurredAt = text(row.get("occurred_at"), now());
            return new Object[]{
                    id,
                    "LEDGER" + String.format("%010d", Math.max(0L, id)),
                    userId,
                    userId,
                    changeType,
                    text(row.getOrDefault("biz_type_code", "MANUAL_ADJUST"), "MANUAL_ADJUST"),
                    "SYSTEM",
                    direction,
                    change,
                    before,
                    after,
                    occurredAt,
                    "SYSTEM",
                    text(row.getOrDefault("note", ""), "")
            };
        });
        markDeletedByIds("pm_point_ledger", collectIds(rows));
    }

    private Map<Long, String> syncOrders(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_exchange_order " +
                "(id, tenant_id, app_id, order_no, user_id, order_type_code, product_type_code, order_status_code, pay_status_code, delivery_status_code, total_point_amount, total_item_count, user_remark, admin_remark, reject_reason, idempotent_no, request_no, submit_at, audit_at, ship_at, finish_at, cancel_at, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,'wxapp',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), order_status_code=VALUES(order_status_code), delivery_status_code=VALUES(delivery_status_code), total_point_amount=VALUES(total_point_amount), total_item_count=VALUES(total_item_count), user_remark=VALUES(user_remark), admin_remark=VALUES(admin_remark), reject_reason=VALUES(reject_reason), submit_at=VALUES(submit_at), audit_at=VALUES(audit_at), ship_at=VALUES(ship_at), finish_at=VALUES(finish_at), cancel_at=VALUES(cancel_at), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long orderId = toLong(row.get("id"));
            String orderNo = text(row.getOrDefault("order_no", "EO" + orderId), "EO" + orderId);
            String orderStatus = normalizeOrderStatus(text(row.getOrDefault("order_status_code", "PENDING_AUDIT"), "PENDING_AUDIT"));
            String deliveryStatus = mapDeliveryStatus(orderStatus, text(row.getOrDefault("delivery_status_code", ""), ""));
            String submitAt = text(row.getOrDefault("submit_at", now()), now());
            return new Object[]{
                    orderId,
                    orderNo,
                    toLong(row.get("user_id")),
                    "EXCHANGE",
                    text(row.getOrDefault("product_type_code", "PHYSICAL"), "PHYSICAL"),
                    orderStatus,
                    "NOT_APPLICABLE",
                    deliveryStatus,
                    toLong(row.getOrDefault("total_point_amount", 0)),
                    Math.max(1, toInt(row.getOrDefault("total_item_count", 1), 1)),
                    text(row.getOrDefault("user_remark", row.getOrDefault("remark", "")), ""),
                    text(row.getOrDefault("admin_remark", ""), ""),
                    text(row.getOrDefault("reject_reason", ""), ""),
                    nullIfBlank(text(row.getOrDefault("idempotent_no", ""), "")),
                    nullIfBlank(text(row.getOrDefault("request_no", ""), "")),
                    submitAt,
                    nullIfBlank(text(row.getOrDefault("audit_at", ""), "")),
                    nullIfBlank(text(row.getOrDefault("ship_at", ""), "")),
                    nullIfBlank(text(row.getOrDefault("finish_at", ""), "")),
                    nullIfBlank(text(row.getOrDefault("cancel_at", ""), ""))
            };
        });
        markDeletedByIds("pm_exchange_order", collectIds(rows));
        return rows == null ? new LinkedHashMap<>() : rows.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        item -> toLong(item.get("id")),
                        item -> text(item.getOrDefault("order_no", ""), ""),
                        (a, b) -> StringUtils.hasText(a) ? a : b,
                        LinkedHashMap::new
                ));
    }

    private void syncOrderItems(List<Map<String, Object>> rows, Map<Long, String> orderNoMap, List<Map<String, Object>> orders) {
        Map<Long, Map<String, Object>> orderMap = orders == null ? new LinkedHashMap<>() : orders.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(item -> toLong(item.get("id")), item -> item, (a, b) -> a, LinkedHashMap::new));
        final String upsertSql = "INSERT INTO pm_exchange_order_item " +
                "(id, tenant_id, order_id, order_no, user_id, spu_id, sku_id, product_no, product_name_snapshot, product_type_code, main_image_snapshot, unit_point_price, quantity, total_point_amount, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE order_id=VALUES(order_id), order_no=VALUES(order_no), user_id=VALUES(user_id), spu_id=VALUES(spu_id), sku_id=VALUES(sku_id), product_name_snapshot=VALUES(product_name_snapshot), unit_point_price=VALUES(unit_point_price), quantity=VALUES(quantity), total_point_amount=VALUES(total_point_amount), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long id = toLong(row.get("id"));
            long orderId = toLong(row.get("order_id"));
            Map<String, Object> order = orderMap.get(orderId);
            long userId = order == null ? toLong(row.get("user_id")) : toLong(order.get("user_id"));
            String orderNo = text(orderNoMap.get(orderId), text(order == null ? null : order.get("order_no"), "EO" + orderId));
            long spuId = toLong(row.getOrDefault("spu_id", 0));
            Object skuId = toLong(row.getOrDefault("sku_id", 0));
            if ((long) skuId <= 0) skuId = null;
            int quantity = Math.max(1, toInt(row.getOrDefault("quantity", 1), 1));
            long unitPoint = toLong(row.getOrDefault("point_price", row.getOrDefault("unit_point_price", 0)));
            long totalPoint = toLong(row.getOrDefault("total_point_amount", unitPoint * quantity));
            return new Object[]{
                    id,
                    orderId,
                    orderNo,
                    userId,
                    spuId,
                    skuId,
                    "SPU" + String.format("%06d", Math.max(0L, spuId)),
                    text(row.getOrDefault("spu_name", row.getOrDefault("product_name_snapshot", "")), ""),
                    text(row.getOrDefault("product_type_code", "PHYSICAL"), "PHYSICAL"),
                    text(row.getOrDefault("main_image_snapshot", ""), ""),
                    unitPoint,
                    quantity,
                    totalPoint
            };
        });
        markDeletedByIds("pm_exchange_order_item", collectIds(rows));
    }

    private void syncOrderFlows(List<Map<String, Object>> rows, Map<Long, String> orderNoMap) {
        final String upsertSql = "INSERT INTO pm_order_flow " +
                "(id, tenant_id, order_id, order_no, from_status_code, to_status_code, action_code, action_reason, operator_type_code, operator_id, operated_at, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE order_id=VALUES(order_id), order_no=VALUES(order_no), from_status_code=VALUES(from_status_code), to_status_code=VALUES(to_status_code), action_code=VALUES(action_code), action_reason=VALUES(action_reason), operated_at=VALUES(operated_at), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long orderId = toLong(row.get("order_id"));
            String orderNo = text(orderNoMap.get(orderId), "EO" + orderId);
            String toStatus = normalizeOrderStatus(text(row.getOrDefault("to_status", row.getOrDefault("to_status_code", "PENDING_AUDIT")), "PENDING_AUDIT"));
            String fromStatus = normalizeOrderStatus(text(row.getOrDefault("from_status", row.getOrDefault("from_status_code", "")), ""));
            String actionCode = normalizeActionCode(text(row.getOrDefault("action_code", row.getOrDefault("action_text", "UPDATE")), "UPDATE"));
            return new Object[]{
                    toLong(row.get("id")),
                    orderId,
                    orderNo,
                    nullIfBlank(fromStatus),
                    toStatus,
                    actionCode,
                    text(row.getOrDefault("note", row.getOrDefault("action_reason", "")), ""),
                    "ADMIN",
                    0,
                    text(row.getOrDefault("occurred_at", row.getOrDefault("operated_at", now())), now())
            };
        });
        markDeletedByIds("pm_order_flow", collectIds(rows));
    }

    private void syncOrderDeliveries(List<Map<String, Object>> rows, Map<Long, String> orderNoMap) {
        final String upsertSql = "INSERT INTO pm_order_delivery " +
                "(id, tenant_id, order_id, order_no, delivery_type_code, logistics_company, logistics_no, shipper_phone, shipped_at, signed_at, delivery_status_code, remark, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE order_id=VALUES(order_id), order_no=VALUES(order_no), logistics_company=VALUES(logistics_company), logistics_no=VALUES(logistics_no), shipper_phone=VALUES(shipper_phone), shipped_at=VALUES(shipped_at), signed_at=VALUES(signed_at), delivery_status_code=VALUES(delivery_status_code), remark=VALUES(remark), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> {
            long orderId = toLong(row.get("order_id"));
            String orderNo = text(orderNoMap.get(orderId), "EO" + orderId);
            String shippedAt = text(row.getOrDefault("ship_at", row.getOrDefault("shipped_at", "")), "");
            return new Object[]{
                    orderId,
                    orderId,
                    orderNo,
                    "EXPRESS",
                    text(row.getOrDefault("express_company", row.getOrDefault("company_name", "")), ""),
                    text(row.getOrDefault("express_no", row.getOrDefault("tracking_no", "")), ""),
                    nullIfBlank(text(row.getOrDefault("shipper_phone", ""), "")),
                    nullIfBlank(shippedAt),
                    nullIfBlank(text(row.getOrDefault("signed_at", ""), "")),
                    text(row.getOrDefault("delivery_status_code", StringUtils.hasText(shippedAt) ? "SHIPPED" : "PENDING"), StringUtils.hasText(shippedAt) ? "SHIPPED" : "PENDING"),
                    text(row.getOrDefault("receiver_address", row.getOrDefault("remark", "")), "")
            };
        });
        markDeletedByIds("pm_order_delivery", collectIds(rows, "order_id"));
    }

    private void syncRecommendSlots(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_recommend_slot " +
                "(id, tenant_id, slot_code, slot_name, page_code, scene_code, status_code, sort_no, remark, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE slot_code=VALUES(slot_code), slot_name=VALUES(slot_name), page_code=VALUES(page_code), scene_code=VALUES(scene_code), status_code=VALUES(status_code), sort_no=VALUES(sort_no), remark=VALUES(remark), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> new Object[]{
                toLong(row.get("id")),
                text(row.getOrDefault("slot_code", ""), ""),
                text(row.getOrDefault("slot_name", ""), ""),
                text(row.getOrDefault("page_code", inferPageCode(row)), inferPageCode(row)),
                nullIfBlank(text(row.getOrDefault("scene_code", ""), "")),
                normalizeEnabledStatus(text(row.getOrDefault("status_code", "ENABLED"), "ENABLED")),
                toInt(row.getOrDefault("sort_no", 0), 0),
                text(row.getOrDefault("remark", ""), "")
        });
        markDeletedByIds("pm_recommend_slot", collectIds(rows));
    }

    private void syncRecommendItems(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_recommend_item " +
                "(id, tenant_id, slot_id, target_type_code, target_id, title, sub_title, image_url, sort_no, valid_start_at, valid_end_at, status_code, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE slot_id=VALUES(slot_id), target_type_code=VALUES(target_type_code), target_id=VALUES(target_id), title=VALUES(title), sub_title=VALUES(sub_title), image_url=VALUES(image_url), sort_no=VALUES(sort_no), valid_start_at=VALUES(valid_start_at), valid_end_at=VALUES(valid_end_at), status_code=VALUES(status_code), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> new Object[]{
                toLong(row.get("id")),
                toLong(row.get("slot_id")),
                text(row.getOrDefault("target_type_code", "SPU"), "SPU"),
                toLong(row.getOrDefault("target_id", row.getOrDefault("spu_id", 0))),
                text(row.getOrDefault("title", row.getOrDefault("product_name", "")), ""),
                text(row.getOrDefault("sub_title", ""), ""),
                text(row.getOrDefault("image_url", row.getOrDefault("banner_image_url", "")), ""),
                toInt(row.getOrDefault("sort_no", 0), 0),
                nullIfBlank(text(row.getOrDefault("start_at", ""), "")),
                nullIfBlank(text(row.getOrDefault("end_at", ""), "")),
                normalizeEnabledStatus(text(row.getOrDefault("status_code", "ENABLED"), "ENABLED"))
        });
        markDeletedByIds("pm_recommend_item", collectIds(rows));
    }

    private void syncSystemConfigs(List<Map<String, Object>> rows) {
        final String upsertSql = "INSERT INTO pm_system_config " +
                "(id, tenant_id, config_key, config_name, config_value, value_type_code, group_code, status_code, remark, created_by, updated_by, is_deleted) " +
                "VALUES (?,0,?,?,?,?,?,?,?,0,0,0) " +
                "ON DUPLICATE KEY UPDATE config_name=VALUES(config_name), config_value=VALUES(config_value), value_type_code=VALUES(value_type_code), group_code=VALUES(group_code), status_code=VALUES(status_code), remark=VALUES(remark), is_deleted=0, deleted_at=NULL, updated_at=CURRENT_TIMESTAMP(3)";
        batch(rows, upsertSql, row -> new Object[]{
                toLong(row.get("id")),
                text(row.getOrDefault("config_key", ""), ""),
                text(row.getOrDefault("config_name", ""), ""),
                text(row.getOrDefault("config_value", ""), ""),
                text(row.getOrDefault("value_type", row.getOrDefault("value_type_code", "STRING")), "STRING"),
                text(row.getOrDefault("group_code", "DEFAULT"), "DEFAULT"),
                normalizeEnabledStatus(text(row.getOrDefault("status_code", "ENABLED"), "ENABLED")),
                text(row.getOrDefault("remark", ""), "")
        });
        markDeletedByIds("pm_system_config", collectIds(rows));
    }

    private void batch(List<Map<String, Object>> rows, String sql, java.util.function.Function<Map<String, Object>, Object[]> mapper) {
        if (!available() || rows == null || rows.isEmpty()) return;
        List<Object[]> args = rows.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!args.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, args);
        }
    }

    private void markDeletedByIds(String table, List<Long> ids) {
        if (!available() || !StringUtils.hasText(table)) return;
        if (ids == null || ids.isEmpty()) {
            jdbcTemplate.update("UPDATE " + table + " SET is_deleted=1, deleted_at=NOW(3) WHERE tenant_id=0");
            return;
        }
        String placeholders = ids.stream().map(item -> "?").collect(Collectors.joining(","));
        String sql = "UPDATE " + table + " SET is_deleted=1, deleted_at=NOW(3) WHERE tenant_id=0 AND id NOT IN (" + placeholders + ")";
        jdbcTemplate.update(sql, ids.toArray());
    }

    private List<Long> collectIds(List<Map<String, Object>> rows) {
        if (rows == null) return new ArrayList<>();
        return rows.stream()
                .map(item -> toLong(item == null ? null : item.get("id")))
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> collectIds(List<Map<String, Object>> rows, String field) {
        if (rows == null) return new ArrayList<>();
        return rows.stream()
                .map(item -> toLong(item == null ? null : item.get(field)))
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private int toInt(Object value, int def) {
        if (value == null) return def;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return def;
        }
    }

    private double toMoney(Object value) {
        if (value == null) return 0D;
        double v;
        if (value instanceof Number) {
            v = ((Number) value).doubleValue();
        } else {
            try {
                v = Double.parseDouble(String.valueOf(value).trim());
            } catch (Exception ignore) {
                v = 0D;
            }
        }
        return Math.round(v * 100D) / 100D;
    }

    private boolean toBool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "1".equals(text) || "true".equals(text) || "yes".equals(text) || "on".equals(text);
    }

    private String text(Object value, String def) {
        String next = value == null ? "" : String.valueOf(value).trim();
        return StringUtils.hasText(next) ? next : def;
    }

    private long resolveCategoryId(Map<String, Object> row) {
        long direct = toLong(row == null ? null : row.get("category_id"));
        if (direct > 0) return direct;
        Object rawIds = row == null ? null : row.get("category_ids");
        if (rawIds instanceof Collection<?>) {
            for (Object item : (Collection<?>) rawIds) {
                long id = toLong(item);
                if (id > 0) return id;
            }
        }
        if (rawIds != null) {
            String text = String.valueOf(rawIds);
            for (String part : text.split("[,，\\s]+")) {
                long id = toLong(part);
                if (id > 0) return id;
            }
        }
        return 0L;
    }

    private String normalizeEnabledStatus(String status) {
        String normalized = String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
        return "DISABLED".equals(normalized) ? "DISABLED" : "ENABLED";
    }

    private String normalizeSaleStatus(String status) {
        String normalized = String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
        if ("ENABLED".equals(normalized)) return "ON_SHELF";
        if ("DISABLED".equals(normalized)) return "OFF_SHELF";
        return "ON_SHELF".equals(normalized) ? "ON_SHELF" : "OFF_SHELF";
    }

    private String normalizeUserStatus(String status) {
        String normalized = String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
        return "FROZEN".equals(normalized) ? "FROZEN" : "ACTIVE";
    }

    private String normalizeOrderStatus(String status) {
        String normalized = String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
        if ("CANCELED".equals(normalized)) return "CLOSED";
        if ("CLOSE".equals(normalized)) return "CLOSED";
        return StringUtils.hasText(normalized) ? normalized : "PENDING_AUDIT";
    }

    private String mapDeliveryStatus(String orderStatusCode, String rawDeliveryStatus) {
        String normalized = String.valueOf(rawDeliveryStatus == null ? "" : rawDeliveryStatus).trim().toUpperCase(Locale.ROOT);
        if (StringUtils.hasText(normalized)) return normalized;
        if ("FINISHED".equals(orderStatusCode)) return "SIGNED";
        if ("SHIPPED".equals(orderStatusCode)) return "SHIPPED";
        return "PENDING";
    }

    private String normalizeActionCode(String actionCode) {
        String normalized = String.valueOf(actionCode == null ? "" : actionCode).trim();
        if (!StringUtils.hasText(normalized)) return "UPDATE";
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.contains("提交")) return "SUBMIT";
        if (upper.contains("取消")) return "CANCEL";
        if (upper.contains("驳回")) return "REJECT";
        if (upper.contains("审核通过") || upper.contains("通过")) return "APPROVE";
        if (upper.contains("发货")) return "SHIP";
        if (upper.contains("完成")) return "COMPLETE";
        if (upper.contains("关闭")) return "CLOSE";
        return upper.replaceAll("[^A-Z0-9_]+", "_");
    }

    private String nullIfBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String inferPageCode(Map<String, Object> row) {
        String slotCode = text(row == null ? null : row.get("slot_code"), "");
        String upper = slotCode.toUpperCase(Locale.ROOT);
        if (upper.contains("HOME")) return "HOME";
        if (upper.contains("DETAIL")) return "DETAIL";
        return "MALL";
    }

    private void ensureColumnExists(String tableName, String columnName, String columnDefinition) {
        if (!available()) return;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class,
                    tableName,
                    columnName
            );
            if (count != null && count > 0) return;
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        } catch (Exception e) {
            log.warn("确保字段 {}.{} 失败: {}", tableName, columnName, e.getMessage());
        }
    }

    private String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
