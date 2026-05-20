package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateStudentVisitRecordDTO {

    @NotNull(message = "学生不能为空")
    private Long studentId;

    @NotBlank(message = "回访方式不能为空")
    private String visitMethod;

    @NotBlank(message = "回访结果不能为空")
    private String visitResult;

    @NotBlank(message = "回访内容不能为空")
    @Size(max = 1000, message = "回访内容不能超过1000字")
    private String content;

    private LocalDateTime nextFollowTime;

    private LocalDateTime visitTime;
}
