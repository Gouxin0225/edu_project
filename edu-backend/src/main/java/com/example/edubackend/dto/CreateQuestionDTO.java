package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateQuestionDTO {
    @NotBlank(message = "课程方向不能为空")
    private String courseCategory;

    private String knowledgePoint;

    @NotBlank(message = "题型不能为空")
    private String type;

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @NotBlank(message = "题干内容不能为空")
    private String content;

    private String optionsJson;

    @NotBlank(message = "标准答案不能为空")
    private String standardAnswer;

    private String analysis;
}
