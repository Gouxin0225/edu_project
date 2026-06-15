package com.example.edubackend.dto;

import lombok.Data;

@Data
public class ExamSubmitRequirementVO {
    private Boolean requireSurvey;
    private Boolean surveySubmitted;
    private Boolean surveyExpired;
    private Long surveyId;
    private String surveyTitle;
    private String message;
}
