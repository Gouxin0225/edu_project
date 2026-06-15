package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatSessionVO {
    private Long id;
    private String title;
    private String courseCategory;
    private String knowledgePoint;
    private String userRole;
    private Boolean isTop;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
