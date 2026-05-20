package com.example.edubackend.dto;

import lombok.Data;

@Data
public class OutlineQuestionUpdateDTO {
    private String knowledgePoint;
    private String type;
    private String difficulty;
    private String content;
    private String optionsJson;
    private String standardAnswer;
    private String analysis;
    private Boolean selected;
}
