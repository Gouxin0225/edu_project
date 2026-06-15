package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleTeacherDispatchVO {
    private Long teacherId;
    private String username;
    private String teacherName;
    private String phone;
    private String schoolName;
    private Long profileId;
    private String homeSchoolName;
    private String dispatchStatus;
    private Byte canTravel;
    private Byte isCenterReserve;
    private Integer maxParallelPlans;
    private Integer maxMonthlyDispatchDays;
    private Integer priority;
    private String availableRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ScheduleTeacherSkillVO> skills = new ArrayList<>();
}
