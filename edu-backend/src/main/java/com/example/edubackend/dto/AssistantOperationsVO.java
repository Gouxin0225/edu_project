package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AssistantOperationsVO {

    @Data
    public static class ClassBoard {
        private Long classId;
        private String className;
        private String schoolName;
        private Integer studentCount = 0;
        private Integer activeCount = 0;
        private Integer riskCount = 0;
        private BigDecimal homeworkCompletionRate = BigDecimal.ZERO;
        private BigDecimal examCompletionRate = BigDecimal.ZERO;
        private BigDecimal surveyCompletionRate = BigDecimal.ZERO;
        private List<StudentBrief> students = new ArrayList<>();
    }

    @Data
    public static class StudentBrief {
        private Long studentId;
        private String studentName;
        private String username;
        private String phone;
        private String schoolName;
        private Long classId;
        private String className;
        private LocalDateTime joinTime;
        private LocalDateTime lastActivityTime;
        private LocalDateTime lastVisitTime;
        private LocalDateTime nextFollowTime;
        private Integer riskScore = 0;
    }

    @Data
    public static class StudentProfile {
        private BasicInfo basicInfo = new BasicInfo();
        private LearningState learningState = new LearningState();
        private List<ClassHistoryItem> classHistory = new ArrayList<>();
        private List<RiskItem> risks = new ArrayList<>();
        private List<VisitItem> visits = new ArrayList<>();
        private List<TaskRecord> exams = new ArrayList<>();
        private List<TaskRecord> homework = new ArrayList<>();
        private List<SurveyRecordItem> surveys = new ArrayList<>();
        private List<String> weakPoints = new ArrayList<>();
    }

    @Data
    public static class BasicInfo {
        private Long studentId;
        private String studentName;
        private String username;
        private String phone;
        private String schoolName;
        private Long currentClassId;
        private String currentClassName;
        private LocalDateTime joinTime;
        private LocalDateTime createTime;
    }

    @Data
    public static class LearningState {
        private Integer examCount = 0;
        private Integer homeworkCount = 0;
        private Integer submittedHomeworkCount = 0;
        private Integer submittedExamCount = 0;
        private Integer missingTaskCount = 0;
        private Integer lowScoreCount = 0;
        private Integer switchScreenCount = 0;
        private Integer surveySubmittedCount = 0;
        private Integer surveyMissingCount = 0;
        private Integer aiQuestionCount = 0;
        private Integer mistakeCount = 0;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private LocalDateTime lastActivityTime;
    }

    @Data
    public static class ClassHistoryItem {
        private Long classId;
        private String className;
        private String status;
        private String joinStatus;
        private String joinSource;
        private LocalDateTime joinTime;
        private LocalDateTime leaveTime;
    }

    @Data
    public static class TaskRecord {
        private Long taskId;
        private String title;
        private String type;
        private LocalDateTime deadline;
        private String status;
        private BigDecimal score;
        private Integer totalScore;
        private Integer switchScreenCount = 0;
        private LocalDateTime startTime;
        private LocalDateTime submitTime;
        private String assistantComment;
    }

    @Data
    public static class SurveyRecordItem {
        private Long surveyId;
        private String title;
        private LocalDateTime deadline;
        private Boolean submitted = false;
        private LocalDateTime submitTime;
    }

    @Data
    public static class VisitItem {
        private Long id;
        private String visitMethod;
        private String visitResult;
        private String content;
        private String problemCategory;
        private String conclusion;
        private String attachmentUrl;
        private Boolean resolved = false;
        private LocalDateTime visitTime;
        private LocalDateTime nextFollowTime;
    }

    @Data
    public static class RiskItem {
        private String id;
        private String type;
        private String title;
        private String level;
        private Long studentId;
        private String studentName;
        private Long classId;
        private String className;
        private String sourceType;
        private Long sourceId;
        private String sourceTitle;
        private LocalDateTime sourceTime;
        private String reason;
        private String status = "PENDING";
    }

    @Data
    public static class TodoItem {
        private String id;
        private String type;
        private String title;
        private Long studentId;
        private String studentName;
        private Long classId;
        private String className;
        private String sourceType;
        private Long sourceId;
        private LocalDateTime deadline;
        private String reason;
        private String priority = "MEDIUM";
    }
}
