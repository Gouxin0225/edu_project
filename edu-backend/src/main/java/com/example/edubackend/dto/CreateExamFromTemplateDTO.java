package com.example.edubackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateExamFromTemplateDTO {
    
    @NotNull(message = "模板ID不能为空")
    private Long templateId;
    
    @NotNull(message = "考试标题不能为空")
    private String title;
    
    private String type = "EXAM";
    
    private java.time.LocalDateTime startTime;
    
    @NotNull(message = "截止时间不能为空")
    private java.time.LocalDateTime endTime;
    
    private java.util.List<Long> targetClassIds;
    
    private String settingJson;
    
    private Integer duration;
    
    private Integer totalScore = 100;
    
    private Integer passScore = 60;
}