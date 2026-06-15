package com.example.edubackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiPaperGenerateVO {
    private Integer requestedCount;
    private Integer existingCount;
    private Integer aiGeneratedCount;
    private String message;
    private List<QuestionVO> questions = new ArrayList<>();
}
