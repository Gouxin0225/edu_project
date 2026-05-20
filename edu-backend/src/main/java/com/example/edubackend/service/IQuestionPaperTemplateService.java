package com.example.edubackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.edubackend.dto.CreateExamFromTemplateDTO;
import com.example.edubackend.dto.CreateTemplateDTO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.dto.TemplateQuestionVO;
import com.example.edubackend.dto.TemplateVO;
import com.example.edubackend.entity.QuestionPaperTemplate;

import java.util.List;

public interface IQuestionPaperTemplateService extends IService<QuestionPaperTemplate> {
    
    TemplateVO createTemplate(CreateTemplateDTO dto, Long creatorId, String creatorName);
    
    IPage<TemplateVO> listTemplates(Integer page, Integer size, Long creatorId);
    
    TemplateVO getTemplateById(Long id);
    
    List<TemplateQuestionVO> getTemplateQuestions(Long templateId);
    
    void deleteTemplate(Long id);
    
    ExamVO createExamFromTemplate(CreateExamFromTemplateDTO dto, Long creatorId);
}