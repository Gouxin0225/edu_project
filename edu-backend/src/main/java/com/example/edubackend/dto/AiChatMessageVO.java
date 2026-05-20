package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageVO {
    private Long sessionId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String sessionTitle;
    private String courseCategory;
    private String knowledgePoint;
    private String question;
    private String answer;
    private String mode;
    private String intent;
    private String responseSource;
    private String sourceSummary;
    private String role;
    private String model;
    private String status;
    private LocalDateTime createTime;
}
