package com.example.edubackend.service;

import com.example.edubackend.dto.*;

import java.util.List;

public interface IStudentService {
    
    StudentDashboardVO getDashboard(Long studentId);
    
    List<MistakeVO> getMistakeList(Long studentId, String knowledgePoint, String questionType);
    
    void markMistakeMastered(Long studentId, Long questionId);
    
    ChallengeVO challenge(Long studentId, ChallengeDTO dto);
}