package com.example.edubackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleCampusProgressInitDTO {
    private Long campusGradeId;

    @NotBlank(message = "校区名称不能为空")
    private String schoolName;

    @NotBlank(message = "年级名称不能为空")
    private String gradeName;

    private Long classId;
    private String majorDirection;
    private Integer studentCount;

    @Valid
    @NotEmpty(message = "应上课程不能为空")
    private List<CourseProgressItem> courses;

    @Data
    public static class CourseProgressItem {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;
        private Integer expectedStudentCount;
        private String progressStatus;
        private String remark;
    }
}
