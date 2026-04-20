package com.integral.mall.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtTokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;

    @Value("${mall.jwt.secret:}")
    private String secret;
    @Value("${mall.jwt.app-access-expire-seconds:259200}")
    private long appAccessExpireSeconds;
    @Value("${mall.jwt.app-refresh-expire-seconds:2592000}")
    private long appRefreshExpireSeconds;
    @Value("${mall.jwt.admin-access-expire-seconds:43200}")
    private long adminAccessExpireSeconds;
    @Value("${mall.jwt.admin-refresh-expire-seconds:604800}")
    private long adminRefreshExpireSeconds;

    public JwtTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String issueAppAccessToken(Long userId) {
        return issueToken("app", "access", userId, appAccessExpireSeconds);
    }

    public String issueAppRefreshToken(Long userId) {
        return issueToken("app", "refresh", userId, appRefreshExpireSeconds);
    }

    public String issueAdminAccessToken(Long adminId) {
        return issueToken("admin", "access", adminId, adminAccessExpireSeconds);
    }

    public String issueAdminRefreshToken(Long adminId) {
        return issueToken("admin", "refresh", adminId, adminRefreshExpireSeconds);
    }

    public Long parseAppAccessUserId(String token) {
        return parseTokenId(token, "app", "access");
    }

    public Long parseAppRefreshUserId(String token) {
        return parseTokenId(token, "app", "refresh");
    }

    public Long parseAdminAccessId(String token) {
        return parseTokenId(token, "admin", "access");
    }

    public Long parseAdminRefreshId(String token) {
        return parseTokenId(token, "admin", "refresh");
    }

    private String issueToken(String scope, String tokenType, Long subjectId, long expireSeconds) {
        if (!StringUtils.hasText(secret) || subjectId == null || subjectId <= 0) return "";
        long now = Instant.now().getEpochSecond();
        long exp = now + Math.max(60L, expireSeconds);
        try {
            String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scope", scope);
            payload.put("token_type", tokenType);
            payload.put("uid", subjectId);
            payload.put("iat", now);
            payload.put("exp", exp);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerBase64 = URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadBase64 = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String content = headerBase64 + "." + payloadBase64;
            String signature = sign(content);
            return content + "." + signature;
        } catch (Exception ignore) {
            return "";
        }
    }

    private Long parseTokenId(String token, String expectedScope, String expectedTokenType) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(token)) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String content = parts[0] + "." + parts[1];
            String signature = sign(content);
            if (!signature.equals(parts[2])) return null;

            String payloadJson = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});

            String scope = String.valueOf(payload.getOrDefault("scope", ""));
            String tokenType = String.valueOf(payload.getOrDefault("token_type", ""));
            if (!expectedScope.equals(scope) || !expectedTokenType.equals(tokenType)) return null;

            long exp = toLong(payload.get("exp"));
            if (exp <= 0 || exp < Instant.now().getEpochSecond()) return null;

            long uid = toLong(payload.get("uid"));
            return uid > 0 ? uid : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] signed = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return URL_ENCODER.encodeToString(signed);
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return 0L;
        }
    }
}

