package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminClassStatisticsVO {
    private Overview overview = new Overview();
    private List<ClassModule> classes = new ArrayList<>();

    @Data
    public static class Overview {
        private Integer classCount = 0;
        private Integer activeClassCount = 0;
        private Integer finishedClassCount = 0;
        private Integer studentCount = 0;
        private Integer homeworkCount = 0;
        private Integer examCount = 0;
        private Integer surveyCount = 0;
        private BigDecimal homeworkAverageScore = BigDecimal.ZERO;
        private BigDecimal examAverageScore = BigDecimal.ZERO;
        private BigDecimal surveyAverageScore = BigDecimal.ZERO;
        private Integer homeworkScoreSampleCount = 0;
        private Integer examScoreSampleCount = 0;
        private Integer surveyScoreSampleCount = 0;
    }

    @Data
    public static class ClassModule {
        private Long classId;
        private String className;
        private Byte status;
        private String statusName;
        private Integer studentCount = 0;
        private List<Person> teachers = new ArrayList<>();
        private List<Person> assistants = new ArrayList<>();
        private WorkSummary homeworkSummary = new WorkSummary();
        private List<WorkItem> homeworks = new ArrayList<>();
        private WorkSummary examSummary = new WorkSummary();
        private List<WorkItem> exams = new ArrayList<>();
        private SurveySummary surveySummary = new SurveySummary();
        private List<SurveyItem> surveys = new ArrayList<>();
    }

    @Data
    public static class Person {
        private Long id;
        private String name;
    }

    @Data
    public static class WorkSummary {
        private Integer taskCount = 0;
        private Integer submittedCount = 0;
        private Integer totalRequired = 0;
        private Integer pendingCount = 0;
        private Integer gradedCount = 0;
        private Integer pendingGradeCount = 0;
        private Integer scoreSampleCount = 0;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
    }

    @Data
    public static class WorkItem {
        private Long taskId;
        private String title;
        private LocalDateTime deadline;
        private Integer totalStudents = 0;
        private Integer submittedCount = 0;
        private Integer pendingCount = 0;
        private Integer gradedCount = 0;
        private Integer returnedCount = 0;
        private Integer pendingGradeCount = 0;
        private Integer scoreSampleCount = 0;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
    }

    @Data
    public static class SurveySummary {
        private Integer surveyCount = 0;
        private Integer responseCount = 0;
        private Integer totalRequired = 0;
        private Integer scoreSampleCount = 0;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
    }

    @Data
    public static class SurveyItem {
        private Long surveyId;
        private String title;
        private LocalDateTime deadline;
        private Byte status;
        private String statusName;
        private Long targetTeacherId;
        private String targetTeacherName;
        private Integer totalStudents = 0;
        private Integer responseCount = 0;
        private Integer pendingCount = 0;
        private Integer scoreSampleCount = 0;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
    }
}
