package com.integral.mall.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integral.mall.api.common.ApiResponse;
import com.integral.mall.api.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final JwtTokenService jwtTokenService;

    @Value("${mall.security.jwt-enabled:true}")
    private boolean jwtEnabled;
    @Value("${mall.security.allow-legacy-token:false}")
    private boolean allowLegacyToken;

    public JwtAuthInterceptor(ObjectMapper objectMapper,
                              org.springframework.beans.factory.ObjectProvider<JwtTokenService> jwtTokenServiceProvider) {
        this.objectMapper = objectMapper;
        this.jwtTokenService = jwtTokenServiceProvider.getIfAvailable();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!jwtEnabled) return true;
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) return true;

        if (uri.startsWith("/api/v1/admin/")) {
            if (isAdminPublicPath(uri)) return true;
            Long adminId = parseAdminId(request);
            if (adminId == null || adminId <= 0) {
                writeUnauthorized(response, "管理员登录已过期，请重新登录");
                return false;
            }
            request.setAttribute("auth_admin_id", adminId);
            return true;
        }

        if (uri.startsWith("/api/v1/app/") && requiresAppAuth(uri)) {
            Long userId = parseAppUserId(request);
            if (userId == null || userId <= 0) {
                writeUnauthorized(response, "登录已过期，请重新登录");
                return false;
            }
            request.setAttribute("auth_user_id", userId);
        }
        return true;
    }

    private boolean isAdminPublicPath(String uri) {
        if (uri.startsWith("/api/v1/admin/files/") && uri.endsWith("/content")) {
            return true;
        }
        return "/api/v1/admin/auth/login".equals(uri)
                || "/api/v1/admin/auth/refresh-token".equals(uri)
                || "/api/v1/admin/logistics/kdniao/callback".equals(uri)
                || "/api/v1/admin/logistics/kdniao".equals(uri)
                || "/api/v1/admin/logistics/kdniao/".equals(uri);
    }

    private boolean requiresAppAuth(String uri) {
        return uri.startsWith("/api/v1/app/users/")
                || uri.startsWith("/api/v1/app/addresses/")
                || uri.startsWith("/api/v1/app/exchanges/")
                || uri.startsWith("/api/v1/app/orders/")
                || uri.startsWith("/api/v1/app/points/")
                || uri.startsWith("/api/v1/app/backpack/")
                || uri.startsWith("/api/v1/app/wish-demands")
                || "/api/v1/app/auth/bind-phone".equals(uri)
                || "/api/v1/app/auth/logout".equals(uri);
    }

    private Long parseAdminId(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) return null;
        if (jwtTokenService != null) {
            Long adminId = jwtTokenService.parseAdminAccessId(token);
            if (adminId != null && adminId > 0) {
                return adminId;
            }
        }
        if (allowLegacyToken && token.startsWith("admin-token-")) {
            return 1L;
        }
        return null;
    }

    private Long parseAppUserId(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) return null;
        if (jwtTokenService != null) {
            Long userId = jwtTokenService.parseAppAccessUserId(token);
            if (userId != null && userId > 0) {
                return userId;
            }
        }
        if (allowLegacyToken && token.startsWith("mock-token-")) {
            return 1001L;
        }
        return null;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) return "";
        return auth.substring("Bearer ".length()).trim();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(4010, message)));
    }
}
