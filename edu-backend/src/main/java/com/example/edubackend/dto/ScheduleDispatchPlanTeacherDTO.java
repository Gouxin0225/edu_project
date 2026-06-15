package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScheduleDispatchPlanTeacherDTO {
    private Long id;
    private Long planId;
    private Long teacherId;
    private String teacherRole;
    private BigDecimal matchScore;
    private String matchReason;
    private String assignStatus;
    private LocalDateTime confirmTime;
    private String rejectReason;
}
