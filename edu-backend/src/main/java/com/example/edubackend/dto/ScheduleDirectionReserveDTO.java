package com.example.edubackend.dto;

import lombok.Data;

@Data
public class ScheduleDirectionReserveDTO {
    private Long id;
    private String schoolName;
    private String directionCode;
    private String courseLevel;
    private Integer minTeacherCount;
    private Integer minExpertCount;
    private String status;
    private String remark;
}
