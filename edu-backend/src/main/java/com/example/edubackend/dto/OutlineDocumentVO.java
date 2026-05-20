package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OutlineDocumentVO {
    private Long id;
    private String title;
    private String originalFilename;
    private String courseCategory;
    private String outlineText;
    private String status;
    private LocalDateTime createTime;
}
