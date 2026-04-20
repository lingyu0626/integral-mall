package com.integral.mall.api.service;

import com.integral.mall.api.controller.AdminBusinessController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderAutoCompleteScheduler {

    @Autowired
    private AdminBusinessController adminBusinessController;

    // 每小时执行一次：物流已签收满3天自动完成
    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Shanghai")
    public void autoCompleteSignedOrders() {
        adminBusinessController.autoCompleteSignedOrders();
    }
}
