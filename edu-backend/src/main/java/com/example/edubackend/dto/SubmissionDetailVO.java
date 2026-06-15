package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SubmissionDetailVO {
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private String status;
    private BigDecimal totalScoreGained;
    private Integer switchScreenCount;
    private String submitTime;
    private String gradeTime;
    private List<QuestionAnswerVO> answers;
    
    @Data
    public static class QuestionAnswerVO {
        private Long questionId;
        private String type;
        private String content;
        private String optionsJson;
        private String studentAnswer;
        private String standardAnswer;
        private Boolean isCorrect;
        private BigDecimal scoreGained;
        private Integer questionScore;
        private BigDecimal aiSuggestScore;
        private String aiSuggestDetail;
    }
}
