package com.example.edubackend.dto;

import lombok.Data;

@Data
public class OutlineQuestionVO {
    private Long id;
    private Long documentId;
    private String courseCategory;
    private String knowledgePoint;
    private String type;
    private String difficulty;
    private String content;
    private String optionsJson;
    private String standardAnswer;
    private String analysis;
    private Boolean selected;
    private Long questionId;
}
