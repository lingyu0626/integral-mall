package com.integral.mall.api.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppCatalogMapper {

    private final JdbcTemplate jdbcTemplate;

    public AppCatalogMapper(org.springframework.beans.factory.ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
    }

    public boolean available() {
        return jdbcTemplate != null;
    }

    public List<Map<String, Object>> listCategories() {
        if (!available()) return new ArrayList<>();
        String sql = "SELECT id, category_name, sort_no, status_code " +
                "FROM pm_category WHERE tenant_id=0 AND is_deleted=0 ORDER BY sort_no ASC, id ASC";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("category_name", rs.getString("category_name"));
                row.put("sort_no", rs.getInt("sort_no"));
                row.put("status_code", rs.getString("status_code"));
                return row;
            });
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listProducts() {
        if (!available()) return new ArrayList<>();
        String sql = "SELECT s.id, s.category_id, c.category_name, s.product_name, s.product_type_code, " +
                "s.point_price, s.stock_available, s.sale_status_code, s.recommend_flag, " +
                "COALESCE(NULLIF(s.main_image_url,''), (" +
                "SELECT pm.media_url FROM pm_product_media pm " +
                "WHERE pm.tenant_id=0 AND pm.is_deleted=0 AND pm.spu_id=s.id AND UPPER(pm.media_type_code)='IMAGE' " +
                "ORDER BY pm.sort_no DESC, pm.id DESC LIMIT 1" +
                "), '') AS main_image_url, " +
                "s.detail_html, " +
                "s.limit_per_user " +
                "FROM pm_product_spu s " +
                "LEFT JOIN pm_category c ON c.id = s.category_id AND c.tenant_id=0 AND c.is_deleted=0 " +
                "WHERE s.tenant_id=0 AND s.is_deleted=0";
        return queryForList(sql);
    }

    public Map<String, Object> getProduct(Long productId) {
        if (!available() || productId == null || productId <= 0) return null;
        String sql = "SELECT s.id, s.category_id, c.category_name, s.product_name, s.product_type_code, " +
                "s.point_price, s.stock_available, s.sale_status_code, s.recommend_flag, " +
                "COALESCE(NULLIF(s.main_image_url,''), (" +
                "SELECT pm.media_url FROM pm_product_media pm " +
                "WHERE pm.tenant_id=0 AND pm.is_deleted=0 AND pm.spu_id=s.id AND UPPER(pm.media_type_code)='IMAGE' " +
                "ORDER BY pm.sort_no DESC, pm.id DESC LIMIT 1" +
                "), '') AS main_image_url, " +
                "s.detail_html, " +
                "s.limit_per_user " +
                "FROM pm_product_spu s " +
                "LEFT JOIN pm_category c ON c.id = s.category_id AND c.tenant_id=0 AND c.is_deleted=0 " +
                "WHERE s.tenant_id=0 AND s.is_deleted=0 AND s.id=?";
        List<Map<String, Object>> rows = queryForList(sql, productId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> listProductSkus(Long productId) {
        if (!available() || productId == null || productId <= 0) return new ArrayList<>();
        String sql = "SELECT id, spu_id, sku_name, point_price, stock_available, sale_status_code " +
                "FROM pm_product_sku WHERE tenant_id=0 AND is_deleted=0 AND spu_id=? ORDER BY id ASC";
        try {
            return jdbcTemplate.query(sql, new Object[]{productId}, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                long skuId = rs.getLong("id");
                row.put("id", skuId);
                row.put("sku_id", skuId);
                row.put("spu_id", rs.getLong("spu_id"));
                row.put("sku_name", rs.getString("sku_name"));
                row.put("point_price", rs.getLong("point_price"));
                row.put("stock_available", rs.getInt("stock_available"));
                String status = normalizeSkuStatus(rs.getString("sale_status_code"));
                row.put("status_code", status);
                row.put("sale_status_code", status);
                return row;
            });
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> queryForList(String sql, Object... args) {
        try {
            return jdbcTemplate.query(sql, args, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("category_id", rs.getLong("category_id"));
                row.put("category_name", rs.getString("category_name"));
                row.put("product_name", rs.getString("product_name"));
                row.put("product_type_code", rs.getString("product_type_code"));
                row.put("point_price", rs.getLong("point_price"));
                row.put("stock_available", rs.getLong("stock_available"));
                String saleStatus = String.valueOf(rs.getString("sale_status_code"));
                row.put("status_code", normalizeSaleStatus(saleStatus));
                row.put("sale_status_code", normalizeSaleStatus(saleStatus));
                row.put("recommend_flag", rs.getInt("recommend_flag") == 1);
                row.put("main_image_url", rs.getString("main_image_url"));
                row.put("detail_html", rs.getString("detail_html"));
                row.put("limit_per_user", rs.getInt("limit_per_user"));
                return row;
            });
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    private String normalizeSaleStatus(String saleStatus) {
        if (!StringUtils.hasText(saleStatus)) return "OFF_SHELF";
        String normalized = saleStatus.trim().toUpperCase();
        if ("ENABLED".equals(normalized)) return "ON_SHELF";
        if ("DISABLED".equals(normalized)) return "OFF_SHELF";
        return "ON_SHELF".equals(normalized) ? "ON_SHELF" : "OFF_SHELF";
    }

    private String normalizeSkuStatus(String saleStatus) {
        if (!StringUtils.hasText(saleStatus)) return "DISABLED";
        String normalized = saleStatus.trim().toUpperCase();
        if ("ON_SHELF".equals(normalized) || "ENABLED".equals(normalized)) return "ENABLED";
        return "DISABLED";
    }
}
