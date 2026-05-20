package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TemplateVO {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Long courseId;
    
    private String courseName;
    
    private Integer totalScore;
    
    private Integer questionCount;
    
    private Long creatorId;
    
    private String creatorName;
    
    private LocalDateTime createdAt;
    
    private List<TemplateQuestionVO> questions;
}