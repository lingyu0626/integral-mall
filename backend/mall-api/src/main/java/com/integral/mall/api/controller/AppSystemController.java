package com.integral.mall.api.controller;

import com.integral.mall.api.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/app")
public class AppSystemController {

    private final AppApiController appApiController;

    public AppSystemController(AppApiController appApiController) {
        this.appApiController = appApiController;
    }

    @GetMapping("/customer-service/contact")
    public ApiResponse<Map<String, Object>> getCustomerServiceContact() {
        return appApiController.getCustomerServiceContact();
    }

    @GetMapping("/dict/{dictTypeCode}/items")
    public ApiResponse<Map<String, Object>> getDictItems(@PathVariable String dictTypeCode,
                                                         @RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "100") int pageSize) {
        return appApiController.getDictItems(dictTypeCode, pageNo, pageSize);
    }

    @GetMapping("/system-configs/public")
    public ApiResponse<Map<String, Object>> getPublicConfigs(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "100") int pageSize) {
        return appApiController.getPublicConfigs(pageNo, pageSize);
    }

    @PostMapping("/wish-demands")
    public ApiResponse<Map<String, Object>> createWishDemand(HttpServletRequest request,
                                                             @RequestBody(required = false) Map<String, Object> payload) {
        return appApiController.createWishDemand(request, payload);
    }

    @GetMapping("/wish-demands")
    public ApiResponse<Map<String, Object>> listWishDemands(HttpServletRequest request,
                                                            @RequestParam(defaultValue = "1") int pageNo,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        return appApiController.listWishDemands(request, pageNo, pageSize);
    }
}

