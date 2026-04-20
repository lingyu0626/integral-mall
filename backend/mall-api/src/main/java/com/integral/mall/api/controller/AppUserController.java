package com.integral.mall.api.controller;

import com.integral.mall.api.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/app")
public class AppUserController {

    private final AppApiController appApiController;

    public AppUserController(AppApiController appApiController) {
        this.appApiController = appApiController;
    }

    @PostMapping("/auth/wx-login")
    public ApiResponse<Map<String, Object>> wxLogin(@RequestBody(required = false) Map<String, Object> payload) {
        return appApiController.wxLogin(payload);
    }

    @PostMapping("/auth/bind-phone")
    public ApiResponse<Map<String, Object>> bindPhone(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        return appApiController.bindPhone(request, payload);
    }

    @PostMapping("/auth/refresh-token")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody(required = false) Map<String, Object> payload) {
        return appApiController.refreshToken(payload);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        return appApiController.logout(request);
    }

    @GetMapping("/users/me")
    public ApiResponse<Map<String, Object>> getMe(HttpServletRequest request) {
        return appApiController.getMe(request);
    }

    @PutMapping("/users/me")
    public ApiResponse<Map<String, Object>> updateMe(HttpServletRequest request,
                                                     @RequestBody(required = false) Map<String, Object> payload) {
        return appApiController.updateMe(request, payload);
    }

    @GetMapping("/users/me/summary")
    public ApiResponse<Map<String, Object>> getMeSummary(HttpServletRequest request) {
        return appApiController.getMeSummary(request);
    }

    @GetMapping("/points/account")
    public ApiResponse<Map<String, Object>> getPointAccount(HttpServletRequest request) {
        return appApiController.getPointAccount(request);
    }

    @GetMapping("/points/ledger")
    public ApiResponse<Map<String, Object>> getPointLedger(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return appApiController.getPointLedger(request, pageNo, pageSize);
    }

    @GetMapping("/points/ledger/{ledgerId}")
    public ApiResponse<Map<String, Object>> getPointLedgerDetail(HttpServletRequest request, @PathVariable Long ledgerId) {
        return appApiController.getPointLedgerDetail(request, ledgerId);
    }

    @GetMapping("/backpack/assets")
    public ApiResponse<Map<String, Object>> getBackpackAssets(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        return appApiController.getBackpackAssets(request, pageNo, pageSize);
    }

    @GetMapping("/backpack/assets/{assetId}")
    public ApiResponse<Map<String, Object>> getBackpackAssetDetail(HttpServletRequest request, @PathVariable Long assetId) {
        return appApiController.getBackpackAssetDetail(request, assetId);
    }

    @GetMapping("/backpack/assets/{assetId}/flows")
    public ApiResponse<Map<String, Object>> getBackpackAssetFlows(HttpServletRequest request,
                                                                  @PathVariable Long assetId,
                                                                  @RequestParam(defaultValue = "1") int pageNo,
                                                                  @RequestParam(defaultValue = "50") int pageSize) {
        return appApiController.getBackpackAssetFlows(request, assetId, pageNo, pageSize);
    }

    @PostMapping("/backpack/assets/{assetId}/use")
    public ApiResponse<Map<String, Object>> useBackpackAsset(HttpServletRequest request, @PathVariable Long assetId) {
        return appApiController.useBackpackAsset(request, assetId);
    }
}

