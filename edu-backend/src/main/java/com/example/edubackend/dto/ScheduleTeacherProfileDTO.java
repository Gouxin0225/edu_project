package com.example.edubackend.dto;

import lombok.Data;

@Data
public class ScheduleTeacherProfileDTO {
    private Long id;
    private Long teacherId;
    private String homeSchoolName;
    private String dispatchMode;
    private String dispatchStatus;
    private Integer maxParallelPlans;
    private Integer maxMonthlyDispatchDays;
    private Byte canTravel;
    private Byte allowCrossSchool;
    private Byte isCenterReserve;
    private Integer priority;
    private String availableRemark;
    private String status;
}
