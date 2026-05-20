package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentReviewNoteVO {

    private Long id;

    private Long studentId;

    private Long sourceMessageId;

    private String courseCategory;

    private String knowledgePoint;

    private String title;

    private String content;

    private String sourceType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
