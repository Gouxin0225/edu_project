package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleDispatchPlanDTO {
    private Long id;
    private String planNo;
    private Long campusGradeId;
    private Long courseId;
    private String planType;
    private String planStatus;
    private String priority;
    private LocalDate expectedStartDate;
    private LocalDate expectedEndDate;
    private String dispatchReason;
    private String requirementDesc;
    private Long createdBy;
    private LocalDateTime submittedTime;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String cancelReason;
}
