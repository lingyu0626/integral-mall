package com.integral.mall.api.service;

import com.integral.mall.api.store.InMemoryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserBalanceReportService {

    private static final Logger log = LoggerFactory.getLogger(UserBalanceReportService.class);
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${mall.report-dir:./data/reports}")
    private String reportDir;

    public synchronized String generateDailyReport(List<Map<String, Object>> users, String trigger) {
        try {
            LocalDate today = LocalDate.now(BEIJING_ZONE);
            String fileName = "user-balance-" + today.format(DAY) + ".csv";
            Path target = ensureReportRoot().resolve(fileName).normalize();
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFF");
            sb.append("导出日期,用户ID,昵称,碎片余额,触发方式,导出时间\n");
            List<Map<String, Object>> sorted = users == null
                    ? new ArrayList<>()
                    : users.stream()
                    .sorted(Comparator.comparingLong(item -> InMemoryData.toLong(item.getOrDefault("id", 0))))
                    .collect(Collectors.toList());
            String nowText = LocalDateTime.now(BEIJING_ZONE).format(DT);
            for (Map<String, Object> user : sorted) {
                sb.append(csvCell(today.toString())).append(',')
                        .append(csvCell(user.get("id"))).append(',')
                        .append(csvCell(user.get("nick_name"))).append(',')
                        .append(csvCell(user.get("point_balance"))).append(',')
                        .append(csvCell(StringUtils.hasText(trigger) ? trigger : "AUTO")).append(',')
                        .append(csvCell(nowText)).append('\n');
            }
            Files.write(target, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return fileName;
        } catch (Exception ex) {
            log.warn("导出用户碎片余额报表失败: {}", ex.getMessage());
            return null;
        }
    }

    public synchronized List<Map<String, Object>> listReports() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            Path root = ensureReportRoot();
            Files.list(root)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName() != null && path.getFileName().toString().startsWith("user-balance-"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception ignore) {
                            return 0;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("file_name", path.getFileName().toString());
                            item.put("file_size_kb", Math.max(1L, (Files.size(path) + 1023L) / 1024L));
                            item.put("updated_at", LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), BEIJING_ZONE).format(DT));
                            result.add(item);
                        } catch (Exception ignore) {
                            // ignore broken file
                        }
                    });
        } catch (Exception ex) {
            log.warn("读取报表列表失败: {}", ex.getMessage());
        }
        return result;
    }

    public synchronized byte[] readReport(String fileName) {
        if (!StringUtils.hasText(fileName)) return null;
        String safeName = fileName.trim();
        if (!safeName.startsWith("user-balance-") || safeName.contains("/") || safeName.contains("\\") || safeName.contains("..")) {
            return null;
        }
        try {
            Path target = ensureReportRoot().resolve(safeName).normalize();
            if (!Files.exists(target) || !Files.isRegularFile(target)) return null;
            return Files.readAllBytes(target);
        } catch (Exception ex) {
            return null;
        }
    }

    private Path ensureReportRoot() throws Exception {
        String configured = StringUtils.hasText(reportDir) ? reportDir : "./data/reports";
        Path root = Paths.get(configured);
        if (!root.isAbsolute()) {
            root = Paths.get(System.getProperty("user.dir", ".")).resolve(root).normalize();
        } else {
            root = root.normalize();
        }
        Files.createDirectories(root);
        return root;
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.contains(",") && !text.contains("\"") && !text.contains("\n") && !text.contains("\r")) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}

