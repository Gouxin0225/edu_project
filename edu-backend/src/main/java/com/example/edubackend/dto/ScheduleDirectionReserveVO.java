package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleDirectionReserveVO {
    private Long id;
    private String schoolName;
    private String directionCode;
    private String courseLevel;
    private Integer minTeacherCount;
    private Integer minExpertCount;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
