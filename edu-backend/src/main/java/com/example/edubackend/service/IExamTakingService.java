package com.example.edubackend.service;

import com.example.edubackend.dto.ExamProgressVO;
import com.example.edubackend.dto.ExamStartVO;
import com.example.edubackend.dto.ExamSubmitRequirementVO;
import com.example.edubackend.dto.SaveAnswerDTO;

public interface IExamTakingService {

    ExamStartVO startExam(Long examId, Long studentId);

    ExamProgressVO resumeExam(Long examId, Long studentId);

    void saveAnswers(Long examId, Long studentId, SaveAnswerDTO dto);

    ExamSubmitRequirementVO getSubmitRequirement(Long examId, Long studentId);

    void submitExam(Long examId, Long studentId);

    int reportScreenSwitch(Long examId, Long studentId);

    int autoSubmitExpiredExams();
}
