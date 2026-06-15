package com.example.edubackend.dto;

import lombok.Data;

@Data
public class ScheduleCoursePrerequisiteDTO {
    private Long id;
    private Long courseId;
    private Long prerequisiteCourseId;
    private String ruleType;
    private String requiredStatus;
    private String status;
}
