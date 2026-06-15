package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleAdvancedBatchStudentDTO {
    private Long id;
    private Long batchId;
    private Long studentId;
    private Long classId;
    private Long campusGradeId;
    private String joinStatus;
    private String eligibilityStatus;
    private LocalDateTime eligibilityCheckedTime;
    private String ineligibleReason;
    private LocalDateTime joinedTime;
    private LocalDateTime exitTime;
    private String remark;
    private Long createdBy;
}
