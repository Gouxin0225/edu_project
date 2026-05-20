package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TeacherStatisticsVO {

    private Long selectedClassId;
    private String selectedClassName;
    private Overview overview = new Overview();
    private List<ClassOption> classes = new ArrayList<>();
    private List<TaskProgress> recentTasks = new ArrayList<>();
    private List<ClassComparison> classComparisons = new ArrayList<>();
    private List<RiskStudent> riskStudents = new ArrayList<>();
    private List<ScoreDistribution> scoreDistribution = new ArrayList<>();
    private List<ScoreTrend> scoreTrends = new ArrayList<>();
    private List<WeakKnowledgePoint> weakKnowledgePoints = new ArrayList<>();
    private AiUsageSummary aiUsageSummary = new AiUsageSummary();
    private SurveySummary surveySummary = new SurveySummary();

    @Data
    public static class Overview {
        private Integer classCount = 0;
        private Integer studentCount = 0;
        private Integer examCount = 0;
        private Integer homeworkCount = 0;
        private Integer activeTaskCount = 0;
        private Integer pendingGradeCount = 0;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private BigDecimal passRate = BigDecimal.ZERO;
    }

    @Data
    public static class ClassOption {
        private Long classId;
        private String className;
        private Integer studentCount = 0;
    }

    @Data
    public static class TaskProgress {
        private Long taskId;
        private String title;
        private String type;
        private LocalDateTime deadline;
        private Integer totalStudents = 0;
        private Integer submittedCount = 0;
        private Integer gradedCount = 0;
        private Integer pendingCount = 0;
        private Integer pendingGradeCount = 0;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private List<String> targetClassNames = new ArrayList<>();
    }

    @Data
    public static class ClassComparison {
        private Long classId;
        private String className;
        private Integer studentCount = 0;
        private Integer taskCount = 0;
        private BigDecimal submissionRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private Integer riskCount = 0;
    }

    @Data
    public static class RiskStudent {
        private Long studentId;
        private String studentName;
        private String className;
        private Integer missingCount = 0;
        private Integer lowScoreCount = 0;
        private Integer switchScreenCount = 0;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private Integer riskScore = 0;
    }

    @Data
    public static class ScoreDistribution {
        private String range;
        private Integer count = 0;
        private BigDecimal rate = BigDecimal.ZERO;
    }

    @Data
    public static class ScoreTrend {
        private Long taskId;
        private String title;
        private LocalDateTime deadline;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private Integer submittedCount = 0;
        private Integer totalStudents = 0;
    }

    @Data
    public static class WeakKnowledgePoint {
        private String courseCategory;
        private String knowledgePoint;
        private Integer attempts = 0;
        private Integer wrongCount = 0;
        private BigDecimal wrongRate = BigDecimal.ZERO;
        private BigDecimal averageScoreRate = BigDecimal.ZERO;
    }

    @Data
    public static class AiUsageSummary {
        private Integer studentQuestionCount = 0;
        private Integer teacherQuestionCount = 0;
        private Integer activeStudentCount = 0;
        private Integer knowledgeDocumentCount = 0;
        private Integer knowledgeChunkCount = 0;
        private Integer reviewNoteCount = 0;
        private List<AiTopic> topTopics = new ArrayList<>();
    }

    @Data
    public static class AiTopic {
        private String label;
        private Integer count = 0;
    }

    @Data
    public static class SurveySummary {
        private Integer surveyCount = 0;
        private Integer publishedCount = 0;
        private Integer totalResponses = 0;
        private List<SurveyItem> recentSurveys = new ArrayList<>();
    }

    @Data
    public static class SurveyItem {
        private Long surveyId;
        private String title;
        private LocalDateTime deadline;
        private Integer status = 0;
        private Integer totalResponses = 0;
    }
}
