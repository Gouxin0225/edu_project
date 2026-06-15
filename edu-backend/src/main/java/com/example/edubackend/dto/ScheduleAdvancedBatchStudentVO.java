package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleAdvancedBatchStudentVO {
    private Long id;
    private Long batchId;
    private String batchName;
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;
    private Long campusGradeId;
    private String gradeName;
    private String joinStatus;
    private String eligibilityStatus;
    private Boolean eligible;
    private List<String> missingCourses = new ArrayList<>();
    private LocalDateTime eligibilityCheckedTime;
    private String ineligibleReason;
    private LocalDateTime joinedTime;
    private LocalDateTime exitTime;
    private String remark;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
