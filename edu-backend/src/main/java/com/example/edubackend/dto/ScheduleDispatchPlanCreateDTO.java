package com.example.edubackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleDispatchPlanCreateDTO {
    @NotNull(message = "课程进度ID不能为空")
    private Long campusCourseProgressId;

    @NotNull(message = "计划开始日期不能为空")
    private LocalDate plannedStartDate;

    @NotNull(message = "计划结束日期不能为空")
    private LocalDate plannedEndDate;

    private List<Long> teacherIds;
    private String remark;
}
