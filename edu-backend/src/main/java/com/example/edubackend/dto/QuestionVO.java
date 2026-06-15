package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionVO {
    private Long id;
    private String courseCategory;
    private String knowledgePoint;
    private String type;
    private String difficulty;
    private String content;
    private String optionsJson;
    private String standardAnswer;
    private String analysis;
    private Long creatorId;
    private String creatorName;
    private Byte isAiGenerated;
    private Boolean isPublic;
    private LocalDateTime createTime;
}
