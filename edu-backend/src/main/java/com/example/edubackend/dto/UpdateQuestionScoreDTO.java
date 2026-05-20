package com.example.edubackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class UpdateQuestionScoreDTO {
    
    @NotNull(message = "分值不能为空")
    @Min(value = 0, message = "分值不能小于0")
    @Max(value = 100, message = "分值不能大于100")
    private Integer score;
}
