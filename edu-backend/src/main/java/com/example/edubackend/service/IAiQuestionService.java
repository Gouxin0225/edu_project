package com.example.edubackend.service;

import com.example.edubackend.dto.AIGradeSuggestDTO;
import com.example.edubackend.dto.AIGradeSuggestVO;
import com.example.edubackend.dto.AiGenerateQuestionDTO;
import com.example.edubackend.entity.QuestionBank;

import java.util.List;

public interface IAiQuestionService {
    List<QuestionBank> generateQuestions(AiGenerateQuestionDTO dto, Long creatorId);
    
    AIGradeSuggestVO getGradeSuggestion(AIGradeSuggestDTO dto);
}
