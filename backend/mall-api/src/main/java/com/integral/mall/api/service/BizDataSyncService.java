package com.integral.mall.api.service;

import com.integral.mall.api.mapper.BizDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BizDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(BizDataSyncService.class);

    private final BizDataMapper bizDataMapper;

    public BizDataSyncService(BizDataMapper bizDataMapper) {
        this.bizDataMapper = bizDataMapper;
    }

    public boolean available() {
        return bizDataMapper != null && bizDataMapper.available();
    }

    public void syncCoreBusinessTables(List<Map<String, Object>> categories,
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
        try {
            bizDataMapper.syncCoreBusinessData(
                    categories, spus, skus, medias, users,
                    userAddresses, pointLedger, orders, orderItems, orderFlows, orderDeliveries,
                    recommendSlots, recommendItems, systemConfigs
            );
        } catch (Exception e) {
            log.warn("业务表同步失败: {}", e.getMessage());
        }
    }
}
