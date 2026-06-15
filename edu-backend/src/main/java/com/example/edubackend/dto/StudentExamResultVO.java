package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentExamResultVO {
    private Long examId;
    private String examTitle;
    private Integer totalScore;
    private BigDecimal scoreGained;
    private Boolean passed;
    private Integer rank;
    private Integer totalStudents;
    private LocalDateTime submitTime;
    private Boolean showAnalysis;
    private List<QuestionResultVO> answers;
    
    @Data
    public static class QuestionResultVO {
        private Long questionId;
        private String type;
        private String difficulty;
        private String content;
        private String optionsJson;
        private Integer score;
        private String standardAnswer;
        private String studentAnswer;
        private Boolean isCorrect;
        private BigDecimal scoreGained;
        private String analysis;
        private BigDecimal aiSuggestScore;
        private String aiSuggestDetail;
    }
}
