package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleCoursePrerequisiteVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long prerequisiteCourseId;
    private String prerequisiteCourseName;
    private String ruleType;
    private String requiredStatus;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
