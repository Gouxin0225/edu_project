package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentDTO {
    
    @NotBlank(message = "批注内容不能为空")
    private String comment;
}