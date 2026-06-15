package com.example.edubackend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleAdvancedBatchStudentImportDTO {
    @NotEmpty(message = "学生ID列表不能为空")
    private List<Long> studentIds;
}
