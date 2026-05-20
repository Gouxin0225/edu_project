package com.example.edubackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChallengeVO {
    
    private String challengeId;
    private List<ChallengeQuestionVO> questions;
    
    @Data
    public static class ChallengeQuestionVO {
        private Long questionId;
        private String courseCategory;
        private String knowledgePoint;
        private String type;
        private String difficulty;
        private String content;
        private String optionsJson;
        private String standardAnswer;
        private String analysis;
    }
}