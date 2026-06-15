package com.example.edubackend.service;

import com.example.edubackend.dto.CreateExamDTO;
import com.example.edubackend.dto.ExamStatisticsVO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.entity.AssessmentTask;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IAssessmentTaskService extends IService<AssessmentTask> {

    ExamVO createExam(CreateExamDTO dto, Long creatorId);

    ExamVO updateExam(Long examId, CreateExamDTO dto, Long userId);

    void addQuestions(Long examId, List<Long> questionIds);

    void removeQuestion(Long examId, Long questionId);

    void autoScore(Long examId);

    void updateQuestionScore(Long examId, Long questionId, Integer score);

    void publishExam(Long examId);

    ExamStatisticsVO getStatistics(Long examId);
}
