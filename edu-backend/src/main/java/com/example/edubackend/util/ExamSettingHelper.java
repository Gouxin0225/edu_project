package com.example.edubackend.util;

import com.example.edubackend.entity.AssessmentTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSettingHelper {

    private static final int DEFAULT_DURATION_MINUTES = 90;
    private static final int DEFAULT_PASS_SCORE = 60;
    private static final int DEFAULT_SWITCH_SCREEN_LIMIT = 5;

    private final ObjectMapper objectMapper;

    public String buildSettingJson(String rawSettingJson, Integer duration, Integer passScore, Boolean published) {
        Map<String, Object> settings = readSettings(rawSettingJson);
        settings.put("duration", normalizePositive(duration, DEFAULT_DURATION_MINUTES));
        settings.put("passScore", normalizePositive(passScore, DEFAULT_PASS_SCORE));
        if (published != null) {
            settings.put("published", published);
        } else if (!settings.containsKey("published")) {
            settings.put("published", false);
        }
        return writeSettings(settings);
    }

    public String markPublished(String rawSettingJson) {
        Map<String, Object> settings = readSettings(rawSettingJson);
        settings.put("published", true);
        return writeSettings(settings);
    }

    public boolean isPublished(AssessmentTask exam) {
        Boolean published = getBoolean(exam, "published");
        return Boolean.TRUE.equals(published);
    }

    public String calculateStatus(AssessmentTask exam) {
        if (!isPublished(exam)) {
            return "DRAFT";
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return "ENDED";
        }
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return "SCHEDULED";
        }
        return "PUBLISHED";
    }

    public int getDuration(AssessmentTask exam) {
        Integer duration = getInteger(exam, "duration");
        if (duration != null && duration > 0) {
            return duration;
        }
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            long minutes = Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
            if (minutes > 0 && minutes <= Integer.MAX_VALUE) {
                return (int) minutes;
            }
        }
        return DEFAULT_DURATION_MINUTES;
    }

    public int getPassScore(AssessmentTask exam) {
        Integer passScore = getInteger(exam, "passScore");
        if (passScore != null && passScore > 0) {
            return passScore;
        }
        if (exam.getTotalScore() != null && exam.getTotalScore() < DEFAULT_PASS_SCORE) {
            return exam.getTotalScore();
        }
        return DEFAULT_PASS_SCORE;
    }

    public int getSwitchScreenLimit(AssessmentTask exam) {
        Integer limit = getInteger(exam, "switchScreenLimit");
        if (limit != null && limit > 0) {
            return limit;
        }
        return DEFAULT_SWITCH_SCREEN_LIMIT;
    }

    public boolean isShowAnalysis(AssessmentTask exam) {
        Boolean showAnalysis = getBoolean(exam, "showAnalysis");
        if (showAnalysis != null) {
            return showAnalysis;
        }
        showAnalysis = getBoolean(exam, "show_analysis");
        return showAnalysis == null || showAnalysis;
    }

    public int getRemainingSeconds(AssessmentTask exam, LocalDateTime startedAt, LocalDateTime now) {
        LocalDateTime deadline = resolveExamDeadline(exam, startedAt);
        if (deadline == null) {
            return getDuration(exam) * 60;
        }
        long remaining = Duration.between(now, deadline).getSeconds();
        return (int) Math.max(remaining, 0);
    }

    public LocalDateTime resolveExamDeadline(AssessmentTask exam, LocalDateTime startedAt) {
        LocalDateTime deadline = exam.getEndTime();
        if (startedAt != null) {
            LocalDateTime durationDeadline = startedAt.plusMinutes(getDuration(exam));
            if (deadline == null || durationDeadline.isBefore(deadline)) {
                deadline = durationDeadline;
            }
        }
        return deadline;
    }

    public Map<String, Object> readSettings(String rawSettingJson) {
        if (rawSettingJson == null || rawSettingJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(rawSettingJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse exam setting_json", e);
            return new LinkedHashMap<>();
        }
    }

    private Integer getInteger(AssessmentTask exam, String key) {
        Object value = readSettings(exam.getSettingJson()).get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBoolean(AssessmentTask exam, String key) {
        Object value = readSettings(exam.getSettingJson()).get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        return null;
    }

    private int normalizePositive(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private String writeSettings(Map<String, Object> settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize exam settings", e);
        }
    }
}
