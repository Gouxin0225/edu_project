package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleCampusCourseProgressVO {
    private Long id;
    private Long campusGradeId;
    private String schoolName;
    private String gradeName;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Integer expectedStudentCount;
    private String progressStatus;
    private BigDecimal progressPercent;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Long currentTeacherId;
    private String currentTeacherName;
    private Long lastPlanId;
    private Long completionConfirmedBy;
    private LocalDateTime completionConfirmedTime;
    private String remark;
    private Boolean canSchedule;
    private List<String> blockedReasons = new ArrayList<>();
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
