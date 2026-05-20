package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateHomeworkDTO {
    
    @NotBlank(message = "作业标题不能为空")
    private String title;
    
    @NotBlank(message = "作业内容不能为空")
    private String content;
    
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;
    
    private List<Long> targetClassIds;
    
    private String attachments;
}
