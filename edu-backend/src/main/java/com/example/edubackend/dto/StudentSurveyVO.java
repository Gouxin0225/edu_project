package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentSurveyVO {
    
    private Long surveyId;
    private String title;
    private LocalDateTime endTime;
    private String targetTeacherName;
    private Byte isAnonymousRequired;
    private String status;
    private Boolean alreadySubmitted;
    private Integer totalQuestions;
}