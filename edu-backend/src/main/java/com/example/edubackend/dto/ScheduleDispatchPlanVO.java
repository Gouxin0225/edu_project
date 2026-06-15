package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleDispatchPlanVO {
    private Long id;
    private String planNo;
    private Long campusGradeId;
    private String gradeName;
    private Long courseId;
    private String courseName;
    private String planType;
    private String planStatus;
    private String priority;
    private LocalDate expectedStartDate;
    private LocalDate expectedEndDate;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String dispatchReason;
    private String remark;
    private String requirementDesc;
    private Integer suggestedTeacherCount;
    private String warning;
    private List<String> warnings = new ArrayList<>();
    private List<ScheduleDispatchPlanTeacherVO> teachers = new ArrayList<>();
    private Long createdBy;
    private String createdByName;
    private LocalDateTime submittedTime;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
