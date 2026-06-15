package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AssistantSurveyResponseVO {
    private Long surveyId;
    private String title;
    private Byte isAnonymousRequired;
    private Integer totalStudents = 0;
    private Integer submittedCount = 0;
    private Integer unsubmittedCount = 0;
    private List<ResponseItem> responses = new ArrayList<>();
    private List<StudentItem> unsubmittedStudents = new ArrayList<>();

    @Data
    public static class ResponseItem {
        private Long recordId;
        private Long studentId;
        private String studentName;
        private String username;
        private String className;
        private LocalDateTime submitTime;
        private List<AnswerItem> answers = new ArrayList<>();
    }

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String title;
        private String type;
        private String answerValue;
    }

    @Data
    public static class StudentItem {
        private Long studentId;
        private String studentName;
        private String username;
        private String className;
    }
}
