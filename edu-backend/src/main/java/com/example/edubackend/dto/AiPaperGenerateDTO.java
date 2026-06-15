package com.example.edubackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiPaperGenerateDTO {
    @NotBlank(message = "课程方向不能为空")
    private String courseCategory;

    private String knowledgePoint;

    private String context;

    @NotEmpty(message = "题型不能为空")
    private List<String> types = new ArrayList<>();

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @Positive(message = "数量必须大于0")
    @Max(value = 100, message = "一次最多生成100题")
    private Integer count = 10;

    private List<Long> excludeQuestionIds = new ArrayList<>();

    private Boolean preferExisting = true;
}
