package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleCampusCourseProgressDTO {
    private Long id;
    private Long campusGradeId;
    private Long courseId;
    private Integer expectedStudentCount;
    private String progressStatus;
    private BigDecimal progressPercent;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Long currentTeacherId;
    private Long lastPlanId;
    private Long completionConfirmedBy;
    private LocalDateTime completionConfirmedTime;
    private String remark;
}
