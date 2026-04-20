package com.integral.mall.api.controller;

import com.integral.mall.api.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/app")
public class AppProductController {

    private final AppApiController appApiController;

    public AppProductController(AppApiController appApiController) {
        this.appApiController = appApiController;
    }

    @GetMapping("/home/recommends")
    public ApiResponse<Map<String, Object>> homeRecommends(HttpServletRequest request) {
        return appApiController.homeRecommends(request);
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, Object>> getCategories(@RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "100") int pageSize) {
        return appApiController.getCategories(pageNo, pageSize);
    }

    @GetMapping("/categories/{categoryId}/products")
    public ApiResponse<Map<String, Object>> getCategoryProducts(@PathVariable Long categoryId,
                                                                @RequestParam(defaultValue = "1") int pageNo,
                                                                @RequestParam(defaultValue = "20") int pageSize,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false, name = "sort_by") String sortBy) {
        return appApiController.getCategoryProducts(categoryId, pageNo, pageSize, keyword, sortBy);
    }

    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> searchProducts(@RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize,
                                                           @RequestParam(required = false) String keyword) {
        return appApiController.searchProducts(pageNo, pageSize, keyword);
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<Map<String, Object>> getProductDetail(@PathVariable Long productId) {
        return appApiController.getProductDetail(productId);
    }

    @GetMapping("/products/{productId}/exchange-preview")
    public ApiResponse<Map<String, Object>> getExchangePreview(HttpServletRequest request,
                                                               @PathVariable Long productId,
                                                               @RequestParam(required = false, name = "sku_id") Long skuId) {
        return appApiController.getExchangePreview(request, productId, skuId);
    }

    @GetMapping("/group-resources")
    public ApiResponse<Map<String, Object>> getGroupResources(@RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        return appApiController.getGroupResources(pageNo, pageSize);
    }

    @GetMapping("/group-resources/{resourceId}")
    public ApiResponse<Map<String, Object>> getGroupResourceDetail(@PathVariable Long resourceId) {
        return appApiController.getGroupResourceDetail(resourceId);
    }
}

