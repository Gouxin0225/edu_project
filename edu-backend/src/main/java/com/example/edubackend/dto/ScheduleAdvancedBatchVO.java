package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleAdvancedBatchVO {
    private Long id;
    private String batchNo;
    private String batchName;
    private Long courseId;
    private String courseName;
    private String schoolName;
    private Long planId;
    private String planNo;
    private String batchStatus;
    private LocalDate expectedStartDate;
    private LocalDate expectedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Integer capacity;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
