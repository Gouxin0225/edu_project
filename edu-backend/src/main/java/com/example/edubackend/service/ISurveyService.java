package com.example.edubackend.service;

import com.example.edubackend.dto.*;
import com.example.edubackend.entity.SurveyTask;

import java.util.List;

public interface ISurveyService {
    
    Long createSurvey(CreateSurveyDTO dto, Long creatorId);
    
    void publishSurvey(Long surveyId);

    void deleteSurvey(Long surveyId);
    
    List<SurveyListVO> getSurveyList(Long creatorId, String role);
    
    List<StudentSurveyVO> getStudentSurveyList(Long studentId);
    
    SurveyDetailVO getSurveyDetail(Long surveyId);
    
    void submitSurvey(Long studentId, SubmitSurveyDTO dto, String clientIp);
    
    SurveyStatisticsVO getSurveyStatistics(Long surveyId);
}
