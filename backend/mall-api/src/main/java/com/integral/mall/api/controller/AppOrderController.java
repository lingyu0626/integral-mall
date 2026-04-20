package com.integral.mall.api.controller;

import com.integral.mall.api.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/app")
public class AppOrderController {

    private final AppApiController appApiController;

    public AppOrderController(AppApiController appApiController) {
        this.appApiController = appApiController;
    }

    @GetMapping("/addresses")
    public ApiResponse<Map<String, Object>> getAddresses(HttpServletRequest request,
                                                         @RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        return appApiController.getAddresses(request, pageNo, pageSize);
    }

    @PostMapping("/addresses")
    public ApiResponse<Map<String, Object>> createAddress(HttpServletRequest request,
                                                          @RequestBody Map<String, Object> payload) {
        return appApiController.createAddress(request, payload);
    }

    @GetMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> getAddressDetail(HttpServletRequest request, @PathVariable Long addressId) {
        return appApiController.getAddressDetail(request, addressId);
    }

    @PutMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> updateAddress(HttpServletRequest request,
                                                          @PathVariable Long addressId,
                                                          @RequestBody Map<String, Object> payload) {
        return appApiController.updateAddress(request, addressId, payload);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> deleteAddress(HttpServletRequest request, @PathVariable Long addressId) {
        return appApiController.deleteAddress(request, addressId);
    }

    @PutMapping("/addresses/{addressId}/default")
    public ApiResponse<Map<String, Object>> setDefaultAddress(HttpServletRequest request, @PathVariable Long addressId) {
        return appApiController.setDefaultAddress(request, addressId);
    }

    @PostMapping("/exchanges/orders")
    public ApiResponse<Map<String, Object>> submitExchangeOrder(HttpServletRequest request,
                                                                @RequestBody Map<String, Object> payload) {
        return appApiController.submitExchangeOrder(request, payload);
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> getOrders(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String order_status_code) {
        return appApiController.getOrders(request, pageNo, pageSize, order_status_code);
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> getOrderDetail(HttpServletRequest request, @PathVariable Long orderId) {
        return appApiController.getOrderDetail(request, orderId);
    }

    @GetMapping("/orders/{orderId}/flows")
    public ApiResponse<Map<String, Object>> getOrderFlows(HttpServletRequest request,
                                                          @PathVariable Long orderId,
                                                          @RequestParam(defaultValue = "1") int pageNo,
                                                          @RequestParam(defaultValue = "50") int pageSize) {
        return appApiController.getOrderFlows(request, orderId, pageNo, pageSize);
    }

    @GetMapping("/orders/{orderId}/delivery")
    public ApiResponse<Map<String, Object>> getOrderDelivery(HttpServletRequest request, @PathVariable Long orderId) {
        return appApiController.getOrderDelivery(request, orderId);
    }

    @GetMapping("/orders/{orderId}/logistics-traces")
    public ApiResponse<Map<String, Object>> getOrderLogisticsTraces(HttpServletRequest request, @PathVariable Long orderId) {
        return appApiController.getOrderLogisticsTraces(request, orderId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<Map<String, Object>> cancelOrder(HttpServletRequest request, @PathVariable Long orderId) {
        return appApiController.cancelOrder(request, orderId);
    }

    @PostMapping("/orders/{orderId}/reject-decision")
    public ApiResponse<Map<String, Object>> decideRejectedOrder(HttpServletRequest request,
                                                                @PathVariable Long orderId,
                                                                @RequestBody(required = false) Map<String, Object> payload) {
        return appApiController.decideRejectedOrder(request, orderId, payload);
    }

    @GetMapping("/orders/status-counts")
    public ApiResponse<Map<String, Object>> getOrderStatusCounts(HttpServletRequest request) {
        return appApiController.getOrderStatusCounts(request);
    }
}

