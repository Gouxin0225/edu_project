package com.example.edubackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ChallengeDTO {
    
    @Min(value = 1, message = "题目数量至少为1")
    @Max(value = 20, message = "题目数量最多为20")
    private Integer questionCount;
}