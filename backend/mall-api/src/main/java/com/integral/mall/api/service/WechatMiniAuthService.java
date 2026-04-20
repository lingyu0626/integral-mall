package com.integral.mall.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class WechatMiniAuthService {

    @Value("${mall.wechat.appid:}")
    private String appId;

    @Value("${mall.wechat.secret:}")
    private String appSecret;
    @Value("${mall.wechat.mock-enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String cachedAccessToken = "";
    private volatile long accessTokenExpireAtMs = 0L;

    public WxSession code2Session(String code) {
        if (mockEnabled) {
            String safeCode = String.valueOf(code == null ? "" : code).trim();
            if (!StringUtils.hasText(safeCode)) {
                throw new IllegalStateException("code 不能为空");
            }
            String normalized = safeCode.replaceAll("[^a-zA-Z0-9_-]", "");
            if (!StringUtils.hasText(normalized)) {
                normalized = "mock";
            }
            return new WxSession("mock_openid_" + normalized, "");
        }
        ensureConfigReady();
        if (!StringUtils.hasText(code)) {
            throw new IllegalStateException("code 不能为空");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + encode(appId)
                + "&secret=" + encode(appSecret)
                + "&js_code=" + encode(code.trim())
                + "&grant_type=authorization_code";
        Map<String, Object> resp = readMap(restTemplate.getForObject(url, String.class));
        assertWechatSuccess(resp, "微信登录失败");

        String openId = String.valueOf(resp.getOrDefault("openid", "")).trim();
        if (!StringUtils.hasText(openId)) {
            throw new IllegalStateException("微信登录失败：未返回 openid");
        }
        String unionId = String.valueOf(resp.getOrDefault("unionid", "")).trim();
        return new WxSession(openId, unionId);
    }

    public String resolvePhoneByCode(String phoneCode) {
        if (mockEnabled) {
            if (!StringUtils.hasText(phoneCode)) {
                throw new IllegalStateException("手机号授权 code 不能为空");
            }
            return "1380000" + Math.abs(String.valueOf(phoneCode).hashCode() % 10000);
        }
        ensureConfigReady();
        if (!StringUtils.hasText(phoneCode)) {
            throw new IllegalStateException("手机号授权 code 不能为空");
        }
        Map<String, Object> resp = callGetUserPhone(phoneCode, false);
        if (isTokenInvalid(resp)) {
            resp = callGetUserPhone(phoneCode, true);
        }
        assertWechatSuccess(resp, "获取手机号失败");

        Object phoneInfoRaw = resp.get("phone_info");
        if (!(phoneInfoRaw instanceof Map)) {
            throw new IllegalStateException("获取手机号失败：未返回手机号信息");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> phoneInfo = (Map<String, Object>) phoneInfoRaw;
        String phone = String.valueOf(phoneInfo.getOrDefault("purePhoneNumber", "")).trim();
        if (!StringUtils.hasText(phone)) {
            phone = String.valueOf(phoneInfo.getOrDefault("phoneNumber", "")).trim();
        }
        if (!StringUtils.hasText(phone)) {
            throw new IllegalStateException("获取手机号失败：手机号为空");
        }
        return phone;
    }

    private Map<String, Object> callGetUserPhone(String phoneCode, boolean forceRefreshToken) {
        String accessToken = getAccessToken(forceRefreshToken);
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + encode(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("code", phoneCode.trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        return readMap(response.getBody());
    }

    private String getAccessToken(boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && StringUtils.hasText(cachedAccessToken) && now < accessTokenExpireAtMs) {
            return cachedAccessToken;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + encode(appId)
                + "&secret=" + encode(appSecret);
        Map<String, Object> resp = readMap(restTemplate.getForObject(url, String.class));
        assertWechatSuccess(resp, "微信 access_token 获取失败");

        String accessToken = String.valueOf(resp.getOrDefault("access_token", "")).trim();
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("微信 access_token 获取失败：返回为空");
        }
        int expiresIn = toInt(resp.get("expires_in"), 7200);
        long ttl = Math.max(300L, expiresIn - 120L) * 1000L;
        cachedAccessToken = accessToken;
        accessTokenExpireAtMs = System.currentTimeMillis() + ttl;
        return accessToken;
    }

    private void ensureConfigReady() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new IllegalStateException("微信登录未配置：请在后端配置 mall.wechat.appid 和 mall.wechat.secret");
        }
    }

    private boolean isTokenInvalid(Map<String, Object> resp) {
        int code = toInt(resp.get("errcode"), 0);
        return code == 40001 || code == 42001;
    }

    private void assertWechatSuccess(Map<String, Object> resp, String prefixMessage) {
        int errCode = toInt(resp.get("errcode"), 0);
        if (errCode == 0) return;
        String errMsg = String.valueOf(resp.getOrDefault("errmsg", "unknown"));
        throw new IllegalStateException(prefixMessage + "（errcode=" + errCode + ", errmsg=" + errMsg + "）");
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) return defaultValue;
        return Integer.parseInt(text);
    }

    private Map<String, Object> readMap(String text) {
        if (!StringUtils.hasText(text)) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(text, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("微信接口响应解析失败", e);
        }
    }

    private String encode(String text) {
        return URLEncoder.encode(Objects.toString(text, ""), StandardCharsets.UTF_8);
    }

    public static class WxSession {
        private final String openId;
        private final String unionId;

        public WxSession(String openId, String unionId) {
            this.openId = openId;
            this.unionId = unionId;
        }

        public String getOpenId() {
            return openId;
        }

        public String getUnionId() {
            return unionId;
        }
    }
}
