package com.example.edubackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleTeacherSkillConfigDTO {
    private Long teacherId;

    @Valid
    @NotEmpty(message = "可授课程不能为空")
    private List<CourseSkillItem> courses;

    @Data
    public static class CourseSkillItem {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;
        private String skillLevel;
        private Byte certified;
        private String status;
        private String remark;
    }
}
