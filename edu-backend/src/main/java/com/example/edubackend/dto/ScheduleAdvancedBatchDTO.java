package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleAdvancedBatchDTO {
    private Long id;
    private String batchNo;
    private String batchName;
    private Long courseId;
    private String schoolName;
    private Long planId;
    private String batchStatus;
    private LocalDate expectedStartDate;
    private LocalDate expectedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Integer capacity;
    private Long createdBy;
}
