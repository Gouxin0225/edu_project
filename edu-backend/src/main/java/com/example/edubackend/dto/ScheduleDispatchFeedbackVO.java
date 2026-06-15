package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScheduleDispatchFeedbackVO {
    private Long id;
    private Long planId;
    private String planNo;
    private Long campusGradeId;
    private String gradeName;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String feedbackType;
    private String executionStatus;
    private BigDecimal progressPercent;
    private String feedbackContent;
    private String issueDesc;
    private String nextAction;
    private LocalDateTime feedbackTime;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
