package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitHomeworkDTO {
    
    @NotBlank(message = "作业内容不能为空")
    private String content;
    
    private String gitLink;
    
    private String fileUrl;
}
