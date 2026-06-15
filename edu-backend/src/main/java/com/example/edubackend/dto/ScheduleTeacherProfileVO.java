package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleTeacherProfileVO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String username;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
