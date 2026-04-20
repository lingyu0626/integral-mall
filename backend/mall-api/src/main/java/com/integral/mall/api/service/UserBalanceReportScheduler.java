package com.integral.mall.api.service;

import com.integral.mall.api.controller.AdminBusinessController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserBalanceReportScheduler {

    @Autowired
    private AdminBusinessController adminBusinessController;

    // 每天北京时间 12:00 自动导出一次
    @Scheduled(cron = "0 0 12 * * ?", zone = "Asia/Shanghai")
    public void exportDailyUserBalance() {
        adminBusinessController.generateDailyUserBalanceReport("AUTO");
    }
}

