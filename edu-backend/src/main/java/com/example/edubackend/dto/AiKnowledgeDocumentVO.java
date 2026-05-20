package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiKnowledgeDocumentVO {
    private Long id;
    private String title;
    private String courseCategory;
    private String knowledgePoint;
    private String sourceType;
    private String sourceName;
    private String visibility;
    private String accessScope;
    private Long ownerId;
    private String ownerRole;
    private Integer chunkCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
