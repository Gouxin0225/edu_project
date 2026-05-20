package com.example.edubackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.edubackend.dto.CreateQuestionDTO;
import com.example.edubackend.dto.QuestionVO;
import com.example.edubackend.entity.QuestionBank;

public interface IQuestionBankService extends IService<QuestionBank> {
    QuestionBank createQuestion(CreateQuestionDTO dto, Long creatorId);
    
    QuestionBank updateQuestion(Long id, CreateQuestionDTO dto);
    
    void deleteQuestion(Long id);
    
    IPage<QuestionVO> getQuestionPage(Page<QuestionBank> pageParam, String courseCategory, String type, String difficulty, Long filterCreatorId, Long userId, String userRole);
    
    void publishQuestion(Long id);
}
