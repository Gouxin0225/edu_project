package com.example.edubackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeItemDTO {
    
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
    
    @NotNull(message = "得分不能为空")
    private BigDecimal scoreGained;
    
    private String comment;
}
