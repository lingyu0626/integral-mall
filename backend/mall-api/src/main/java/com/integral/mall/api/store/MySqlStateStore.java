package com.integral.mall.api.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MySqlStateStore {

    private static final Logger log = LoggerFactory.getLogger(MySqlStateStore.class);

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS admin_state_snapshot (" +
                    "snapshot_key VARCHAR(64) PRIMARY KEY," +
                    "snapshot_json LONGTEXT NOT NULL," +
                    "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${mall.require-mysql:false}")
    private boolean requireMysql;

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jdbcUrl) || !StringUtils.hasText(username)) {
            String msg = "MySQL状态存储未启用：缺少 spring.datasource 配置";
            if (requireMysql) {
                throw new IllegalStateException(msg);
            }
            log.warn(msg);
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(CREATE_TABLE_SQL);
            }
            ready.set(true);
            log.info("MySQL状态存储已启用");
        } catch (Exception e) {
            ready.set(false);
            String msg = "MySQL状态存储初始化失败: " + e.getMessage();
            if (requireMysql) {
                throw new IllegalStateException(msg, e);
            }
            log.warn("{}，将降级为内存模式", msg);
        }
    }

    public boolean isReady() {
        return ready.get();
    }

    public Optional<String> load(String snapshotKey) {
        if (!isReady() || !StringUtils.hasText(snapshotKey)) return Optional.empty();
        final String sql = "SELECT snapshot_json FROM admin_state_snapshot WHERE snapshot_key = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, snapshotKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.ofNullable(rs.getString("snapshot_json"));
            }
        } catch (Exception e) {
            log.warn("读取快照失败 key={}: {}", snapshotKey, e.getMessage());
            return Optional.empty();
        }
    }

    public void save(String snapshotKey, String snapshotJson) {
        if (!isReady() || !StringUtils.hasText(snapshotKey) || snapshotJson == null) return;
        final String sql = "INSERT INTO admin_state_snapshot(snapshot_key, snapshot_json) VALUES(?, ?) " +
                "ON DUPLICATE KEY UPDATE snapshot_json = VALUES(snapshot_json), updated_at = CURRENT_TIMESTAMP";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, snapshotKey);
            ps.setString(2, snapshotJson);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("保存快照失败 key={}: {}", snapshotKey, e.getMessage());
        }
    }

    public List<Map<String, Object>> loadBizCategories() {
        if (!isReady()) return new ArrayList<>();
        final String sql = "SELECT id, category_name, sort_no, status_code, updated_at " +
                "FROM pm_category WHERE is_deleted = 0 ORDER BY sort_no DESC, id DESC";
        return queryForList(sql);
    }

    public List<Map<String, Object>> loadBizSpus() {
        if (!isReady()) return new ArrayList<>();
        final String sql = "SELECT id, product_name, product_type_code, category_id, point_price, stock_available, sale_status_code, recommend_flag, main_image_url, detail_html, limit_per_user, updated_at " +
                "FROM pm_product_spu WHERE is_deleted = 0 ORDER BY id DESC";
        return queryForList(sql);
    }

    public List<Map<String, Object>> loadBizSkus() {
        if (!isReady()) return new ArrayList<>();
        final String sql = "SELECT id, spu_id, sku_name, point_price, stock_available, sale_status_code " +
                "FROM pm_product_sku WHERE is_deleted = 0 ORDER BY id DESC";
        return queryForList(sql);
    }

    public List<Map<String, Object>> loadBizMedias() {
        if (!isReady()) return new ArrayList<>();
        final String sql = "SELECT id, spu_id, media_type_code, media_url, sort_no, status_code " +
                "FROM pm_product_media WHERE is_deleted = 0 ORDER BY spu_id ASC, sort_no DESC, id DESC";
        return queryForList(sql);
    }

    public List<Map<String, Object>> loadBizUsers() {
        if (!isReady()) return new ArrayList<>();
        final String sql = "SELECT id, user_no, nick_name, phone, phone_masked, open_id, union_id, avatar_url, status_code, remark, ext_json, created_at " +
                "FROM pm_user WHERE is_deleted = 0 ORDER BY id ASC";
        return queryForList(sql);
    }

    public List<Map<String, Object>> loadBizPointAccounts() {
        if (!isReady()) return new ArrayList<>();
        final String sql = "SELECT user_id, point_balance, point_total_income, point_total_expense, account_status_code " +
                "FROM pm_point_account WHERE is_deleted = 0";
        return queryForList(sql);
    }

    private List<Map<String, Object>> queryForList(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    list.add(row);
                }
            }
        } catch (Exception e) {
            log.warn("业务表查询失败: {}", e.getMessage());
        }
        return list;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
