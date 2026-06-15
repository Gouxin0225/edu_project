package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleCourseVO {
    private Long id;
    private String courseCode;
    private String courseName;
    private String courseDirection;
    private String courseLevel;
    private String courseType;
    private Integer teacherCapacity;
    private Integer sortOrder;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
