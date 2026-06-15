package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScheduleDispatchFeedbackDTO {
    private Long id;
    private Long planId;
    private Long campusGradeId;
    private Long courseId;
    private Long teacherId;
    private String feedbackType;
    private String executionStatus;
    private BigDecimal progressPercent;
    private String feedbackContent;
    private String issueDesc;
    private String nextAction;
    private LocalDateTime feedbackTime;
    private Long createdBy;
}
