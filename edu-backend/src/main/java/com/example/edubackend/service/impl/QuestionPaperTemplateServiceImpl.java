package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateExamFromTemplateDTO;
import com.example.edubackend.dto.CreateTemplateDTO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.dto.TemplateQuestionVO;
import com.example.edubackend.dto.TemplateVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.entity.TemplateQuestionRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.QuestionPaperTemplateMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.TaskQuestionRelMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.IQuestionBankService;
import com.example.edubackend.service.IQuestionPaperTemplateService;
import com.example.edubackend.service.ITaskQuestionRelService;
import com.example.edubackend.service.ITemplateQuestionRelService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionPaperTemplateServiceImpl extends ServiceImpl<QuestionPaperTemplateMapper, com.example.edubackend.entity.QuestionPaperTemplate> implements IQuestionPaperTemplateService {

    private final ITemplateQuestionRelService templateQuestionRelService;
    private final IQuestionBankService questionBankService;
    private final IAssessmentTaskService assessmentTaskService;
    private final ITaskQuestionRelService taskQuestionRelService;
    private final QuestionBankMapper questionBankMapper;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final ObjectMapper objectMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final ExamSettingHelper examSettingHelper;

    @Override
    @Transactional
    public TemplateVO createTemplate(CreateTemplateDTO dto, Long creatorId, String creatorName) {
        com.example.edubackend.entity.QuestionPaperTemplate template = new com.example.edubackend.entity.QuestionPaperTemplate();
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setCourseId(dto.getCourseId());
        template.setCourseName(dto.getCourseName());
        template.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100);
        template.setQuestionCount(dto.getQuestionIds().size());
        template.setCreatorId(creatorId);
        template.setCreatorName(creatorName);
        
        baseMapper.insert(template);
        
        List<Long> questionIds = dto.getQuestionIds();
        List<Integer> scores = dto.getScores();
        
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            QuestionBank question = questionBankService.getById(questionId);
            if (question == null) {
                throw new BusinessException(404, "题目不存在: " + questionId);
            }
            
            TemplateQuestionRel rel = new TemplateQuestionRel();
            rel.setTemplateId(template.getId());
            rel.setQuestionId(questionId);
            rel.setScore(scores != null && scores.size() > i ? scores.get(i) : 0);
            rel.setSortOrder(i + 1);
            templateQuestionRelService.save(rel);
        }
        
        return getTemplateById(template.getId());
    }

    @Override
    public IPage<TemplateVO> listTemplates(Integer page, Integer size, Long creatorId) {
        Page<com.example.edubackend.entity.QuestionPaperTemplate> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<com.example.edubackend.entity.QuestionPaperTemplate> wrapper = new LambdaQueryWrapper<>();
        if (!"ADMIN".equals(UserContext.getUser().getRole())) {
            wrapper.eq(com.example.edubackend.entity.QuestionPaperTemplate::getCreatorId, creatorId);
        }
        wrapper.orderByDesc(com.example.edubackend.entity.QuestionPaperTemplate::getCreatedAt);
        
        IPage<com.example.edubackend.entity.QuestionPaperTemplate> pageResult = baseMapper.selectPage(pageParam, wrapper);
        
        Page<TemplateVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        List<TemplateVO> voList = new ArrayList<>();
        
        for (com.example.edubackend.entity.QuestionPaperTemplate t : pageResult.getRecords()) {
            TemplateVO vo = toTemplateVO(t);
            voList.add(vo);
        }
        
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public TemplateVO getTemplateById(Long id) {
        com.example.edubackend.entity.QuestionPaperTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        assertCanUseTemplate(template);
        return toTemplateVO(template);
    }

    @Override
    public List<TemplateQuestionVO> getTemplateQuestions(Long templateId) {
        com.example.edubackend.entity.QuestionPaperTemplate template = baseMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        assertCanUseTemplate(template);

        List<TemplateQuestionRel> rels = templateQuestionRelService.list(
                new LambdaQueryWrapper<TemplateQuestionRel>()
                        .eq(TemplateQuestionRel::getTemplateId, templateId)
                        .orderByAsc(TemplateQuestionRel::getSortOrder)
        );
        
        List<Long> questionIds = rels.stream()
                .map(TemplateQuestionRel::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        if (questionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<QuestionBank> questions = questionBankService.listByIds(questionIds);
        Map<Long, QuestionBank> questionMap = questions.stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q));
        
        List<TemplateQuestionVO> result = new ArrayList<>();
        for (TemplateQuestionRel rel : rels) {
            QuestionBank q = questionMap.get(rel.getQuestionId());
            if (q != null) {
                TemplateQuestionVO vo = new TemplateQuestionVO();
                vo.setQuestionId(q.getId());
                vo.setCourseCategory(q.getCourseCategory());
                vo.setKnowledgePoint(q.getKnowledgePoint());
                vo.setType(q.getType());
                vo.setDifficulty(q.getDifficulty());
                vo.setContent(q.getContent());
                vo.setOptionsJson(q.getOptionsJson());
                vo.setStandardAnswer(q.getStandardAnswer());
                vo.setAnalysis(q.getAnalysis());
                vo.setScore(rel.getScore());
                result.add(vo);
            }
        }
        
        return result;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        com.example.edubackend.entity.QuestionPaperTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        assertCanUseTemplate(template);

        templateQuestionRelService.remove(
                new LambdaQueryWrapper<TemplateQuestionRel>()
                        .eq(TemplateQuestionRel::getTemplateId, id)
        );
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ExamVO createExamFromTemplate(CreateExamFromTemplateDTO dto, Long creatorId) {
        com.example.edubackend.entity.QuestionPaperTemplate template = baseMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        if (!"ADMIN".equals(UserContext.getUser().getRole()) && !template.getCreatorId().equals(creatorId)) {
            throw new BusinessException(403, "只能使用自己创建的模板");
        }
        assertCanTargetClasses(dto.getTargetClassIds(), creatorId);
        
        AssessmentTask exam = new AssessmentTask();
        exam.setTitle(dto.getTitle());
        exam.setType("EXAM");
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setTotalScore(template.getTotalScore());
        exam.setCreatorId(creatorId);
        
        if (dto.getTargetClassIds() != null) {
            try {
                exam.setTargetClassIds(objectMapper.writeValueAsString(dto.getTargetClassIds()));
            } catch (Exception e) {
                exam.setTargetClassIds("[]");
            }
        } else {
            exam.setTargetClassIds("[]");
        }
        
        exam.setSettingJson(examSettingHelper.buildSettingJson(
                dto.getSettingJson(), dto.getDuration(), dto.getPassScore(), false));
        assessmentTaskMapper.insert(exam);
        
        List<TemplateQuestionRel> templateRels = templateQuestionRelService.list(
                new LambdaQueryWrapper<TemplateQuestionRel>()
                        .eq(TemplateQuestionRel::getTemplateId, dto.getTemplateId())
                        .orderByAsc(TemplateQuestionRel::getSortOrder)
        );
        
        for (TemplateQuestionRel templateRel : templateRels) {
            TaskQuestionRel rel = new TaskQuestionRel();
            rel.setTaskId(exam.getId());
            rel.setQuestionId(templateRel.getQuestionId());
            rel.setScore(templateRel.getScore());
            rel.setSortOrder(templateRel.getSortOrder());
            taskQuestionRelService.save(rel);
        }
        
        ExamVO vo = new ExamVO();
        vo.setId(exam.getId());
        vo.setTitle(exam.getTitle());
        vo.setType(exam.getType());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setTotalScore(exam.getTotalScore());
        vo.setDuration(examSettingHelper.getDuration(exam));
        vo.setPassScore(examSettingHelper.getPassScore(exam));
        vo.setCreatorId(exam.getCreatorId());
        vo.setCreateTime(exam.getCreateTime());
        vo.setStatus(examSettingHelper.calculateStatus(exam));
        
        if (dto.getTargetClassIds() != null) {
            vo.setTargetClassIds(dto.getTargetClassIds());
        }
        
        return vo;
    }

    private void assertCanUseTemplate(com.example.edubackend.entity.QuestionPaperTemplate template) {
        if (!"ADMIN".equals(UserContext.getUser().getRole())
                && !template.getCreatorId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只能访问自己创建的模板");
        }
    }

    private void assertCanTargetClasses(List<Long> targetClassIds, Long creatorId) {
        if ("ADMIN".equals(UserContext.getUser().getRole())) {
            return;
        }
        if (targetClassIds == null || targetClassIds.isEmpty()) {
            throw new BusinessException(400, "目标班级不能为空");
        }
        List<Long> allowedClassIds = teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().eq(TeacherClassRel::getTeacherId, creatorId)
        ).stream().map(TeacherClassRel::getClassId).toList();
        if (!allowedClassIds.containsAll(targetClassIds)) {
            throw new BusinessException(403, "只能发布到自己负责的班级");
        }
    }
    
    private TemplateVO toTemplateVO(com.example.edubackend.entity.QuestionPaperTemplate template) {
        TemplateVO vo = new TemplateVO();
        vo.setId(template.getId());
        vo.setName(template.getName());
        vo.setDescription(template.getDescription());
        vo.setCourseId(template.getCourseId());
        vo.setCourseName(template.getCourseName());
        vo.setTotalScore(template.getTotalScore());
        vo.setQuestionCount(template.getQuestionCount());
        vo.setCreatorId(template.getCreatorId());
        vo.setCreatorName(template.getCreatorName());
        vo.setCreatedAt(template.getCreatedAt());
        return vo;
    }
}
