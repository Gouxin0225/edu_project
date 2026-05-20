package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateExamDTO {
    
    @NotBlank(message = "考试标题不能为空")
    private String title;
    
    private String type = "EXAM";
    
    private LocalDateTime startTime;
    
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime endTime;
    
    private List<Long> targetClassIds;
    
    private String settingJson;

    private Integer duration;

    private Integer totalScore = 100;

    private Integer passScore = 60;
}
