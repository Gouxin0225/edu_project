package com.example.edubackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateSurveyDTO {
    
    @NotBlank(message = "问卷标题不能为空")
    private String title;
    
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime endTime;
    
    private List<Long> targetClassIds;
    
    private Byte isAnonymousRequired;
    
    private Long targetTeacherId;
    
    @Valid
    private List<QuestionConfig> questions;
    
    @Data
    public static class QuestionConfig {
        @NotBlank(message = "题型不能为空")
        private String type;
        
        @NotBlank(message = "题干不能为空")
        private String title;
        
        private String optionsJson;
        
        private Byte isRequired;
        
        private Integer sortOrder;
    }
}
