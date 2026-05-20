package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateTemplateDTO {
    
    @NotBlank(message = "模板名称不能为空")
    private String name;
    
    private String description;
    
    private Long courseId;
    
    private String courseName;
    
    @NotNull(message = "题目ID列表不能为空")
    private List<Long> questionIds;
    
    private List<Integer> scores;
    
    private Integer totalScore = 100;
}