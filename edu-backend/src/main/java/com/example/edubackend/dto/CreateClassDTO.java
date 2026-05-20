package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClassDTO {
    @NotBlank(message = "班级名称不能为空")
    private String className;

    private Byte status = 1;

    private String schoolName;

    private Byte allowStudentApply = 1;
}
