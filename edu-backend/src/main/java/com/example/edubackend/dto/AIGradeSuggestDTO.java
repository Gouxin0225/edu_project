package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AIGradeSuggestDTO {

    private Long questionId;

    private String questionType;

    @DecimalMin(value = "0.0", message = "满分不能小于0")
    private BigDecimal maxScore = BigDecimal.TEN;
    
    @NotBlank(message = "题目内容不能为空")
    private String questionContent;
    
    @NotBlank(message = "标准答案不能为空")
    private String standardAnswer;
    
    private String studentAnswer;

    private String scoringRubric;
}
