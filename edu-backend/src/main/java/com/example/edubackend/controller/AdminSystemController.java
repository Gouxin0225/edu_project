package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system")
@RequireRole({"ADMIN"})
@RequiredArgsConstructor
public class AdminSystemController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectProvider<TaskScheduler> taskSchedulerProvider;
    private final ApplicationContext applicationContext;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.endpoint:}")
    private String aiEndpoint;

    @Value("${file.upload-dir:/edu-platform/uploads}")
    private String uploadDir;

    @GetMapping("/check")
    public Result<Map<String, Object>> check() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", LocalDateTime.now());
        result.put("database", checkDatabase());
        result.put("redis", checkRedis());
        result.put("aiKey", checkAiKey());
        result.put("uploadDir", checkUploadDir());
        result.put("scheduler", checkScheduler());
        result.put("overallStatus", resolveOverallStatus(result));
        return Result.success(result);
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> item = item("DOWN", "数据库连接失败");
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            item.put("status", valid ? "UP" : "DOWN");
            item.put("message", valid ? "数据库连接正常" : "数据库连接不可用");
            item.put("url", connection.getMetaData().getURL());
        } catch (Exception e) {
            item.put("detail", e.getMessage());
        }
        return item;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> item = item("DOWN", "Redis 连接失败");
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            item.put("status", StringUtils.hasText(pong) ? "UP" : "DOWN");
            item.put("message", StringUtils.hasText(pong) ? "Redis 连接正常" : "Redis 未响应 PING");
        } catch (Exception e) {
            item.put("detail", e.getMessage());
        }
        return item;
    }

    private Map<String, Object> checkAiKey() {
        Map<String, Object> item = item(StringUtils.hasText(aiApiKey) ? "UP" : "WARN",
                StringUtils.hasText(aiApiKey) ? "AI Key 已配置" : "AI Key 未配置");
        item.put("endpoint", StringUtils.hasText(aiEndpoint) ? aiEndpoint : "-");
        return item;
    }

    private Map<String, Object> checkUploadDir() {
        Path path = Path.of(uploadDir);
        boolean exists = Files.exists(path);
        boolean directory = Files.isDirectory(path);
        boolean writable = Files.isWritable(path);
        String status = exists && directory && writable ? "UP" : "DOWN";
        Map<String, Object> item = item(status, status.equals("UP") ? "上传目录可写" : "上传目录不存在或不可写");
        item.put("path", path.toAbsolutePath().toString());
        item.put("exists", exists);
        item.put("directory", directory);
        item.put("writable", writable);
        return item;
    }

    private Map<String, Object> checkScheduler() {
        boolean schedulerBean = taskSchedulerProvider.getIfAvailable() != null;
        String[] scheduledBeanNames = applicationContext.getBeanNamesForAnnotation(org.springframework.scheduling.annotation.EnableScheduling.class);
        boolean enabled = schedulerBean || scheduledBeanNames.length > 0;
        return item(enabled ? "UP" : "WARN", enabled ? "定时任务能力已启用" : "未发现已启用的定时任务配置");
    }

    private Map<String, Object> item(String status, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("status", status);
        item.put("message", message);
        return item;
    }

    private String resolveOverallStatus(Map<String, Object> result) {
        boolean hasDown = result.values().stream().anyMatch(this::isDownItem);
        if (hasDown) {
            return "DOWN";
        }
        boolean hasWarn = result.values().stream().anyMatch(this::isWarnItem);
        return hasWarn ? "WARN" : "UP";
    }

    @SuppressWarnings("unchecked")
    private boolean isDownItem(Object item) {
        return item instanceof Map<?, ?> map && "DOWN".equals(map.get("status"));
    }

    @SuppressWarnings("unchecked")
    private boolean isWarnItem(Object item) {
        return item instanceof Map<?, ?> map && "WARN".equals(map.get("status"));
    }
}
