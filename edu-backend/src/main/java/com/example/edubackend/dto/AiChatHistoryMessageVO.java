package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatHistoryMessageVO {
    private Long id;
    private Long sessionId;
    private String messageRole;
    private String content;
    private String courseCategory;
    private String knowledgePoint;
    private String mode;
    private String intent;
    private String responseSource;
    private String sourceSummary;
    private String model;
    private String status;
    private LocalDateTime createTime;
}
