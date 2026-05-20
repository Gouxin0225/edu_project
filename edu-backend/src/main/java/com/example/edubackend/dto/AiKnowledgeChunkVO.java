package com.example.edubackend.dto;

import lombok.Data;

@Data
public class AiKnowledgeChunkVO {
    private Long chunkId;
    private Long documentId;
    private Integer chunkIndex;
    private String ref;
    private String content;
    private String sourceTitle;
    private String sourceName;
    private String courseCategory;
    private String knowledgePoint;
    private String accessScope;
    private Integer score;
}
