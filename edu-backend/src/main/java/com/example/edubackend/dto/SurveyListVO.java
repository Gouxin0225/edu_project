package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SurveyListVO {
    
    private Long surveyId;
    private String title;
    private LocalDateTime endTime;
    private String targetTeacherName;
    private Byte isAnonymousRequired;
    private Byte status;
    private Integer totalQuestions;
    private Integer totalSubmissions;
}