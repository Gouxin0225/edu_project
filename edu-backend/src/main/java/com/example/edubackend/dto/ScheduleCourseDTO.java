package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleCourseDTO {
    private Long id;

    @NotBlank(message = "课程编码不能为空")
    private String courseCode;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotBlank(message = "课程方向不能为空")
    private String courseDirection;

    @NotBlank(message = "课程层级不能为空")
    private String courseLevel;

    @NotBlank(message = "课程类型不能为空")
    private String courseType;

    @NotNull(message = "讲师承载人数不能为空")
    private Integer teacherCapacity;

    private Integer sortOrder;
    private String status;
    private String remark;
}
