package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnHomeworkDTO {
    
    @NotBlank(message = "打回意见不能为空")
    private String teacherComment;
}
