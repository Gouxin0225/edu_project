package com.example.edubackend.dto;

import lombok.Data;

@Data
public class ScheduleTeacherProfileConfigDTO {
    private Long teacherId;
    private String homeSchoolName;
    private String dispatchStatus;
    private Byte canTravel;
    private Byte isCenterReserve;
    private Integer maxParallelPlans;
    private Integer maxMonthlyDispatchDays;
    private Integer priority;
    private String availableRemark;
}
