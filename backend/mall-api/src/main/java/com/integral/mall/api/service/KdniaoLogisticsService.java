package com.integral.mall.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class KdniaoLogisticsService {

    private static final Logger log = LoggerFactory.getLogger(KdniaoLogisticsService.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mall.logistics.kdniao.enabled:false}")
    private boolean enabled;
    @Value("${mall.logistics.kdniao.business-id:}")
    private String businessId;
    @Value("${mall.logistics.kdniao.api-key:}")
    private String apiKey;
    @Value("${mall.logistics.kdniao.api-url:https://api.kdniao.com/api/dist}")
    private String apiUrl;
    @Value("${mall.logistics.kdniao.callback-url:}")
    private String callbackUrl;
    @Value("${mall.logistics.kdniao.timeout-ms:8000}")
    private int timeoutMs;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(businessId)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiUrl);
    }

    public String getBusinessId() {
        return businessId == null ? "" : businessId;
    }

    public String getCallbackUrl() {
        return callbackUrl == null ? "" : callbackUrl;
    }

    public List<ShipperCandidate> detectShippers(String logisticNo) {
        if (!StringUtils.hasText(logisticNo) || !isConfigured()) return new ArrayList<>();
        try {
            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("LogisticCode", logisticNo.trim());
            JsonNode response = callApi("2002", requestData);
            if (response == null || !response.path("Success").asBoolean(false)) {
                return new ArrayList<>();
            }
            JsonNode shippers = response.path("Shippers");
            if (!shippers.isArray()) return new ArrayList<>();
            List<ShipperCandidate> list = new ArrayList<>();
            for (JsonNode item : shippers) {
                String code = text(item, "ShipperCode");
                String name = text(item, "ShipperName");
                if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) continue;
                double score = parseScore(item.path("ShipperRate").asText(""));
                if (score <= 0D) {
                    score = parseScore(item.path("Rate").asText(""));
                }
                list.add(new ShipperCandidate(code, name, score));
            }
            list.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            return list;
        } catch (Exception ex) {
            log.warn("快递鸟识别物流公司失败: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    public SubscribeResult subscribeTrace(String shipperCode, String logisticNo, String orderNo) {
        if (!StringUtils.hasText(logisticNo)) {
            return SubscribeResult.fail("快递单号不能为空");
        }
        if (!StringUtils.hasText(shipperCode)) {
            return SubscribeResult.fail("物流公司编码不能为空");
        }
        if (!isConfigured()) {
            return SubscribeResult.fail("快递鸟配置未启用");
        }
        if (!StringUtils.hasText(callbackUrl)) {
            return SubscribeResult.fail("未配置快递回调地址 callback-url");
        }
        try {
            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("OrderCode", StringUtils.hasText(orderNo) ? orderNo.trim() : "");
            requestData.put("ShipperCode", shipperCode.trim());
            requestData.put("LogisticCode", logisticNo.trim());
            requestData.put("CustomerName", "");
            requestData.put("MonthCode", "");
            requestData.put("CallBack", callbackUrl.trim());
            requestData.put("Callback", callbackUrl.trim());

            JsonNode response = callApi("1008", requestData);
            if (response == null) {
                return SubscribeResult.fail("订阅失败：无返回");
            }
            boolean success = response.path("Success").asBoolean(false);
            String reason = firstNotBlank(
                    text(response, "Reason"),
                    text(response, "ResultMsg"),
                    text(response, "ReasonMessage")
            );
            if (success) {
                return SubscribeResult.ok(firstNotBlank(reason, "订阅成功"));
            }
            return SubscribeResult.fail(firstNotBlank(reason, "订阅失败"));
        } catch (Exception ex) {
            log.warn("快递鸟订阅物流轨迹失败: {}", ex.getMessage());
            return SubscribeResult.fail("订阅异常：" + ex.getMessage());
        }
    }

    public QueryTraceResult queryTrace(String shipperCode, String logisticNo, String orderNo) {
        if (!StringUtils.hasText(logisticNo)) {
            return QueryTraceResult.fail("快递单号不能为空");
        }
        if (!isConfigured()) {
            return QueryTraceResult.fail("快递鸟配置未启用");
        }

        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("OrderCode", StringUtils.hasText(orderNo) ? orderNo.trim() : "");
        requestData.put("ShipperCode", StringUtils.hasText(shipperCode) ? shipperCode.trim() : "");
        requestData.put("LogisticCode", logisticNo.trim());
        requestData.put("CustomerName", "");
        requestData.put("MonthCode", "");

        List<String> requestTypes = Arrays.asList("8001", "1002");
        QueryTraceResult lastResult = QueryTraceResult.fail("暂无物流轨迹");
        for (String requestType : requestTypes) {
            try {
                JsonNode response = callApi(requestType, requestData);
                QueryTraceResult parsed = parseQueryTraceResult(response, requestType, shipperCode, logisticNo);
                if (parsed.isSuccess() && !parsed.getTraces().isEmpty()) {
                    return parsed;
                }
                lastResult = parsed;
            } catch (Exception ex) {
                log.warn("快递鸟轨迹查询失败(type={}): {}", requestType, ex.getMessage());
                lastResult = QueryTraceResult.fail("查询失败：" + ex.getMessage());
            }
        }
        return lastResult;
    }

    public boolean verifySign(String requestData, String dataSign) {
        if (!isConfigured() || !StringUtils.hasText(requestData) || !StringUtils.hasText(dataSign)) {
            return false;
        }
        try {
            String expectedRaw = buildRawDataSign(requestData);
            String incoming = dataSign.trim();
            if (expectedRaw.equals(incoming)) return true;

            String normalized = incoming.replace(" ", "+");
            if (expectedRaw.equals(normalized)) return true;

            String decoded = URLDecoder.decode(normalized, StandardCharsets.UTF_8.name());
            if (expectedRaw.equals(decoded)) return true;

            String encodedExpected = URLEncoder.encode(expectedRaw, StandardCharsets.UTF_8.name());
            return encodedExpected.equals(incoming) || encodedExpected.equals(normalized);
        } catch (Exception ex) {
            log.warn("快递鸟签名校验失败: {}", ex.getMessage());
            return false;
        }
    }

    public CallbackPayload parseCallback(String requestData) {
        CallbackPayload payload = new CallbackPayload();
        if (!StringUtils.hasText(requestData)) return payload;
        try {
            JsonNode root = objectMapper.readTree(requestData);
            payload.setSuccess(root.path("Success").asBoolean(true));
            payload.setReason(text(root, "Reason"));
            JsonNode dataNode = root.path("Data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                if (root.path("LogisticCode").isMissingNode()) {
                    return payload;
                }
                dataNode = root;
            }
            if (dataNode.isTextual()) {
                String dataText = dataNode.asText("");
                if (StringUtils.hasText(dataText)) {
                    dataNode = objectMapper.readTree(dataText);
                }
            }

            List<CallbackTraceEvent> events = new ArrayList<>();
            if (dataNode.isObject()) {
                events.add(readCallbackEvent(dataNode));
            } else if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    if (item != null && item.isObject()) {
                        events.add(readCallbackEvent(item));
                    }
                }
            }
            payload.setEvents(events);
            return payload;
        } catch (Exception ex) {
            log.warn("解析快递鸟回调数据失败: {}", ex.getMessage());
            return payload;
        }
    }

    public String mapStateToDeliveryStatus(String state) {
        String safe = state == null ? "" : state.trim();
        if ("3".equals(safe)) return "SIGNED";
        if ("2".equals(safe) || "201".equals(safe) || "202".equals(safe)) return "IN_TRANSIT";
        if ("1".equals(safe)) return "PICKED_UP";
        if ("4".equals(safe) || "5".equals(safe) || "6".equals(safe)) return "PROBLEM";
        return "PENDING";
    }

    public String mapStateToText(String state) {
        String safe = state == null ? "" : state.trim();
        if ("0".equals(safe)) return "暂无轨迹";
        if ("1".equals(safe)) return "已揽收";
        if ("2".equals(safe)) return "在途中";
        if ("3".equals(safe)) return "已签收";
        if ("4".equals(safe)) return "问题件";
        if ("5".equals(safe)) return "疑难件";
        if ("6".equals(safe)) return "退件签收";
        return "运输中";
    }

    private CallbackTraceEvent readCallbackEvent(JsonNode node) {
        CallbackTraceEvent event = new CallbackTraceEvent();
        event.setShipperCode(text(node, "ShipperCode"));
        event.setLogisticCode(text(node, "LogisticCode"));
        event.setState(text(node, "State"));
        event.setReason(text(node, "Reason"));

        String bestTime = "";
        String bestStation = "";
        JsonNode traces = node.path("Traces");
        if (traces.isTextual()) {
            try {
                traces = objectMapper.readTree(traces.asText(""));
            } catch (Exception ignore) {
                traces = null;
            }
        }
        List<TraceNode> traceNodes = parseTraceNodes(traces);
        event.setTraces(traceNodes);
        if (traces != null && traces.isArray()) {
            for (JsonNode trace : traces) {
                String time = firstNotBlank(text(trace, "AcceptTime"), text(trace, "time"));
                String station = firstNotBlank(text(trace, "AcceptStation"), text(trace, "context"));
                if (!StringUtils.hasText(time)) continue;
                if (compareTime(time, bestTime) >= 0) {
                    bestTime = time;
                    bestStation = station;
                }
            }
        }
        if (!StringUtils.hasText(bestTime)) {
            bestTime = firstNotBlank(text(node, "AcceptTime"), text(node, "UpdateTime"));
            bestStation = firstNotBlank(text(node, "AcceptStation"), text(node, "Reason"));
        }
        event.setLatestAcceptTime(bestTime);
        event.setLatestAcceptStation(bestStation);
        return event;
    }

    private QueryTraceResult parseQueryTraceResult(JsonNode response,
                                                   String requestType,
                                                   String fallbackShipperCode,
                                                   String fallbackLogisticCode) {
        if (response == null) {
            return QueryTraceResult.fail("无返回数据");
        }
        boolean success = response.path("Success").asBoolean(false);
        String reason = firstNotBlank(
                text(response, "Reason"),
                text(response, "ResultMsg"),
                text(response, "Message")
        );

        JsonNode detailNode = response;
        JsonNode dataNode = response.path("Data");
        if (!dataNode.isMissingNode() && !dataNode.isNull()) {
            if (dataNode.isTextual()) {
                String text = dataNode.asText("");
                if (StringUtils.hasText(text)) {
                    try {
                        dataNode = objectMapper.readTree(text);
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
            if (dataNode.isObject()) {
                detailNode = dataNode;
            } else if (dataNode.isArray()) {
                JsonNode picked = null;
                for (JsonNode item : dataNode) {
                    if (item == null || !item.isObject()) continue;
                    String logisticCode = firstNotBlank(text(item, "LogisticCode"), text(item, "logisticCode"));
                    if (StringUtils.hasText(fallbackLogisticCode) && fallbackLogisticCode.equalsIgnoreCase(logisticCode)) {
                        picked = item;
                        break;
                    }
                    if (picked == null) picked = item;
                }
                if (picked != null) detailNode = picked;
            }
        }

        String logisticCode = firstNotBlank(
                text(detailNode, "LogisticCode"),
                text(detailNode, "logisticCode"),
                fallbackLogisticCode
        );
        String shipperCode = firstNotBlank(
                text(detailNode, "ShipperCode"),
                text(detailNode, "shipperCode"),
                fallbackShipperCode
        );
        String shipperName = firstNotBlank(
                text(detailNode, "ShipperName"),
                text(detailNode, "LogisticName"),
                text(detailNode, "CompanyName"),
                text(response, "ShipperName")
        );
        String shipperPhone = firstNotBlank(
                text(detailNode, "ShipperPhone"),
                text(detailNode, "ShipperTel"),
                text(detailNode, "CourierPhone"),
                text(response, "ShipperPhone")
        );
        String state = firstNotBlank(text(detailNode, "State"), text(response, "State"));
        String statusText = mapStateToText(state);

        List<TraceNode> traces = parseTraceNodes(detailNode.path("Traces"));
        if (traces.isEmpty()) {
            traces = parseTraceNodes(response.path("Traces"));
        }
        if (traces.isEmpty()) {
            String latestTime = firstNotBlank(text(detailNode, "AcceptTime"), text(detailNode, "UpdateTime"));
            String latestStation = firstNotBlank(text(detailNode, "AcceptStation"), text(detailNode, "Reason"), reason);
            if (StringUtils.hasText(latestTime) || StringUtils.hasText(latestStation)) {
                traces.add(new TraceNode(latestTime, latestStation, statusText, ""));
            }
        }
        traces.sort((a, b) -> compareTime(b.getAcceptTime(), a.getAcceptTime()));

        QueryTraceResult result = new QueryTraceResult();
        result.setSuccess(success || !traces.isEmpty());
        result.setReason(reason);
        result.setRequestType(requestType);
        result.setLogisticCode(logisticCode);
        result.setShipperCode(shipperCode);
        result.setShipperName(shipperName);
        result.setShipperPhone(shipperPhone);
        result.setState(state);
        result.setStatusText(statusText);
        result.setTraces(traces);
        if (!result.isSuccess() && !StringUtils.hasText(result.getReason())) {
            result.setReason("快递公司暂无轨迹");
        }
        return result;
    }

    private List<TraceNode> parseTraceNodes(JsonNode tracesNode) {
        if (tracesNode == null || tracesNode.isMissingNode() || tracesNode.isNull()) {
            return new ArrayList<>();
        }
        JsonNode normalized = tracesNode;
        if (normalized.isTextual()) {
            String text = normalized.asText("");
            if (!StringUtils.hasText(text)) return new ArrayList<>();
            try {
                normalized = objectMapper.readTree(text);
            } catch (Exception ignore) {
                return new ArrayList<>();
            }
        }
        if (!normalized.isArray()) return new ArrayList<>();

        List<TraceNode> list = new ArrayList<>();
        for (JsonNode trace : normalized) {
            if (trace == null || !trace.isObject()) continue;
            String time = firstNotBlank(text(trace, "AcceptTime"), text(trace, "time"), text(trace, "ftime"));
            String station = firstNotBlank(text(trace, "AcceptStation"), text(trace, "context"), text(trace, "Remark"));
            String status = firstNotBlank(text(trace, "Status"), text(trace, "status"));
            String location = firstNotBlank(text(trace, "Location"), text(trace, "location"));
            if (!StringUtils.hasText(time) && !StringUtils.hasText(station)) continue;
            list.add(new TraceNode(time, station, status, location));
        }
        list.sort((a, b) -> compareTime(b.getAcceptTime(), a.getAcceptTime()));
        return list;
    }

    private int compareTime(String left, String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) return 0;
        if (StringUtils.hasText(left) && !StringUtils.hasText(right)) return 1;
        if (!StringUtils.hasText(left)) return -1;
        try {
            LocalDateTime ldtLeft = LocalDateTime.parse(normalizeTime(left), DT);
            LocalDateTime ldtRight = LocalDateTime.parse(normalizeTime(right), DT);
            return ldtLeft.compareTo(ldtRight);
        } catch (Exception ignore) {
            return left.compareTo(right);
        }
    }

    private String normalizeTime(String text) {
        if (!StringUtils.hasText(text)) return "";
        String t = text.trim().replace('T', ' ');
        int dot = t.indexOf('.');
        if (dot > 0) t = t.substring(0, dot);
        if (t.endsWith("Z")) t = t.substring(0, t.length() - 1);
        if (t.length() >= 19) return t.substring(0, 19);
        return t;
    }

    private JsonNode callApi(String requestType, Map<String, Object> requestData) throws Exception {
        String requestJson = objectMapper.writeValueAsString(requestData == null ? new LinkedHashMap<>() : requestData);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("RequestData", requestJson);
        form.add("EBusinessID", businessId.trim());
        form.add("RequestType", requestType);
        form.add("DataSign", buildRawDataSign(requestJson));
        form.add("DataType", "2");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        ResponseEntity<String> response = buildRestTemplate().postForEntity(apiUrl.trim(), entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("HTTP " + response.getStatusCodeValue());
        }
        String body = response.getBody();
        if (!StringUtils.hasText(body)) return null;
        return objectMapper.readTree(body);
    }

    private RestTemplate buildRestTemplate() {
        int safeTimeout = Math.max(timeoutMs, 1000);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(safeTimeout);
        factory.setReadTimeout(safeTimeout);
        return new RestTemplate(factory);
    }

    private String buildRawDataSign(String requestData) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest((requestData + apiKey.trim()).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return Base64.getEncoder().encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String text(JsonNode node, String key) {
        if (node == null || !StringUtils.hasText(key)) return "";
        JsonNode raw = node.path(key);
        if (raw.isMissingNode() || raw.isNull()) return "";
        return raw.asText("");
    }

    private String firstNotBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "";
    }

    private double parseScore(String value) {
        if (!StringUtils.hasText(value)) return 0D;
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignore) {
            return 0D;
        }
    }

    public static class ShipperCandidate {
        private final String code;
        private final String name;
        private final double score;

        public ShipperCandidate(String code, String name, double score) {
            this.code = code == null ? "" : code;
            this.name = name == null ? "" : name;
            this.score = score;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public double getScore() {
            return score;
        }
    }

    public static class SubscribeResult {
        private final boolean success;
        private final String message;

        public SubscribeResult(boolean success, String message) {
            this.success = success;
            this.message = message == null ? "" : message;
        }

        public static SubscribeResult ok(String message) {
            return new SubscribeResult(true, message);
        }

        public static SubscribeResult fail(String message) {
            return new SubscribeResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CallbackPayload {
        private boolean success = true;
        private String reason = "";
        private List<CallbackTraceEvent> events = new ArrayList<>();

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason == null ? "" : reason;
        }

        public List<CallbackTraceEvent> getEvents() {
            return events;
        }

        public void setEvents(List<CallbackTraceEvent> events) {
            this.events = events == null ? new ArrayList<>() : events;
        }
    }

    public static class CallbackTraceEvent {
        private String shipperCode = "";
        private String logisticCode = "";
        private String state = "";
        private String reason = "";
        private String latestAcceptTime = "";
        private String latestAcceptStation = "";
        private List<TraceNode> traces = new ArrayList<>();

        public String getShipperCode() {
            return shipperCode;
        }

        public void setShipperCode(String shipperCode) {
            this.shipperCode = shipperCode == null ? "" : shipperCode;
        }

        public String getLogisticCode() {
            return logisticCode;
        }

        public void setLogisticCode(String logisticCode) {
            this.logisticCode = logisticCode == null ? "" : logisticCode;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state == null ? "" : state;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason == null ? "" : reason;
        }

        public String getLatestAcceptTime() {
            return latestAcceptTime;
        }

        public void setLatestAcceptTime(String latestAcceptTime) {
            this.latestAcceptTime = latestAcceptTime == null ? "" : latestAcceptTime;
        }

        public String getLatestAcceptStation() {
            return latestAcceptStation;
        }

        public void setLatestAcceptStation(String latestAcceptStation) {
            this.latestAcceptStation = latestAcceptStation == null ? "" : latestAcceptStation;
        }

        public List<TraceNode> getTraces() {
            return traces;
        }

        public void setTraces(List<TraceNode> traces) {
            this.traces = traces == null ? new ArrayList<>() : traces;
        }
    }

    public static class TraceNode {
        private String acceptTime = "";
        private String acceptStation = "";
        private String status = "";
        private String location = "";

        public TraceNode() {
        }

        public TraceNode(String acceptTime, String acceptStation, String status, String location) {
            this.acceptTime = acceptTime == null ? "" : acceptTime;
            this.acceptStation = acceptStation == null ? "" : acceptStation;
            this.status = status == null ? "" : status;
            this.location = location == null ? "" : location;
        }

        public String getAcceptTime() {
            return acceptTime;
        }

        public void setAcceptTime(String acceptTime) {
            this.acceptTime = acceptTime == null ? "" : acceptTime;
        }

        public String getAcceptStation() {
            return acceptStation;
        }

        public void setAcceptStation(String acceptStation) {
            this.acceptStation = acceptStation == null ? "" : acceptStation;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status == null ? "" : status;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location == null ? "" : location;
        }
    }

    public static class QueryTraceResult {
        private boolean success;
        private String reason = "";
        private String requestType = "";
        private String logisticCode = "";
        private String shipperCode = "";
        private String shipperName = "";
        private String shipperPhone = "";
        private String state = "";
        private String statusText = "";
        private List<TraceNode> traces = new ArrayList<>();

        public static QueryTraceResult fail(String reason) {
            QueryTraceResult result = new QueryTraceResult();
            result.setSuccess(false);
            result.setReason(reason);
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason == null ? "" : reason;
        }

        public String getRequestType() {
            return requestType;
        }

        public void setRequestType(String requestType) {
            this.requestType = requestType == null ? "" : requestType;
        }

        public String getLogisticCode() {
            return logisticCode;
        }

        public void setLogisticCode(String logisticCode) {
            this.logisticCode = logisticCode == null ? "" : logisticCode;
        }

        public String getShipperCode() {
            return shipperCode;
        }

        public void setShipperCode(String shipperCode) {
            this.shipperCode = shipperCode == null ? "" : shipperCode;
        }

        public String getShipperName() {
            return shipperName;
        }

        public void setShipperName(String shipperName) {
            this.shipperName = shipperName == null ? "" : shipperName;
        }

        public String getShipperPhone() {
            return shipperPhone;
        }

        public void setShipperPhone(String shipperPhone) {
            this.shipperPhone = shipperPhone == null ? "" : shipperPhone;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state == null ? "" : state;
        }

        public String getStatusText() {
            return statusText;
        }

        public void setStatusText(String statusText) {
            this.statusText = statusText == null ? "" : statusText;
        }

        public List<TraceNode> getTraces() {
            return traces;
        }

        public void setTraces(List<TraceNode> traces) {
            this.traces = traces == null ? new ArrayList<>() : traces;
        }
    }
}
