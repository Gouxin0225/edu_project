package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class AiGenerateQuestionDTO {
    @NotBlank(message = "课程方向不能为空")
    private String courseCategory;

    private String knowledgePoint;

    private String context;

    @NotEmpty(message = "题型不能为空")
    private List<String> types;

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @Positive(message = "数量必须大于0")
    private Integer count = 10;
}
