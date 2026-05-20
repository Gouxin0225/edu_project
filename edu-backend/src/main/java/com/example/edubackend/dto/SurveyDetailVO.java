package com.example.edubackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class SurveyDetailVO {
    
    private Long surveyId;
    private String title;
    private String targetTeacherName;
    private Byte isAnonymousRequired;
    private List<QuestionVO> questions;
    
    @Data
    public static class QuestionVO {
        private Long questionId;
        private String type;
        private String title;
        private String optionsJson;
        private Byte isRequired;
        private Integer sortOrder;
    }
}