package com.integral.mall.api.service;

import com.integral.mall.api.mapper.AppCatalogMapper;
import com.integral.mall.api.store.InMemoryData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppCatalogService {

    private final AppCatalogMapper appCatalogMapper;
    private final InMemoryData inMemoryData;

    public AppCatalogService(AppCatalogMapper appCatalogMapper, InMemoryData inMemoryData) {
        this.appCatalogMapper = appCatalogMapper;
        this.inMemoryData = inMemoryData;
    }

    public List<Map<String, Object>> listCategories() {
        if (appCatalogMapper != null && appCatalogMapper.available()) {
            List<Map<String, Object>> rows = appCatalogMapper.listCategories();
            if (!rows.isEmpty()) return rows;
        }
        return new ArrayList<>(inMemoryData.listCategories());
    }

    public List<Map<String, Object>> listProducts() {
        if (appCatalogMapper != null && appCatalogMapper.available()) {
            List<Map<String, Object>> rows = appCatalogMapper.listProducts();
            if (!rows.isEmpty()) {
                return rows.stream()
                        .filter(item -> "ON_SHELF".equalsIgnoreCase(String.valueOf(item.getOrDefault("status_code", ""))))
                        .collect(Collectors.toList());
            }
        }
        return inMemoryData.listProducts();
    }

    public Map<String, Object> getProduct(Long productId) {
        if (appCatalogMapper != null && appCatalogMapper.available()) {
            Map<String, Object> row = appCatalogMapper.getProduct(productId);
            if (row != null && !row.isEmpty()) {
                Map<String, Object> enriched = new LinkedHashMap<>(row);
                enriched.put("sku_list", appCatalogMapper.listProductSkus(productId));
                return enriched;
            }
        }
        return inMemoryData.getProduct(productId);
    }
}
