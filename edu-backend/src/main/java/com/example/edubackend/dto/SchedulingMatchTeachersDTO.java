package com.example.edubackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SchedulingMatchTeachersDTO {
    @NotNull(message = "课程进度ID不能为空")
    private Long campusCourseProgressId;

    @NotNull(message = "计划开始日期不能为空")
    private LocalDate plannedStartDate;

    @NotNull(message = "计划结束日期不能为空")
    private LocalDate plannedEndDate;
}
