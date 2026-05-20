package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MistakeVO {
    
    private Long mistakeId;
    private Long questionId;
    private String courseCategory;
    private String knowledgePoint;
    private String questionType;
    private String difficulty;
    private String content;
    private String optionsJson;
    private String standardAnswer;
    private String analysis;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private Byte isMastered;
}