package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScheduleTeacherSkillVO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private Long courseId;
    private String courseName;
    private String skillLevel;
    private Byte certified;
    private BigDecimal score;
    private LocalDateTime lastTeachTime;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
