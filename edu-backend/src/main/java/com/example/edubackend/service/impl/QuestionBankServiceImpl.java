package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.dto.CreateQuestionDTO;
import com.example.edubackend.dto.QuestionVO;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.IQuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements IQuestionBankService {

    private final SysUserMapper sysUserMapper;

    @Override
    public QuestionBank createQuestion(CreateQuestionDTO dto, Long creatorId) {
        SysUser creator = sysUserMapper.selectById(creatorId);
        QuestionBank question = new QuestionBank();
        question.setCourseCategory(dto.getCourseCategory());
        question.setKnowledgePoint(textOrEmpty(dto.getKnowledgePoint()));
        question.setType(dto.getType());
        question.setDifficulty(dto.getDifficulty());
        question.setContent(dto.getContent());
        question.setOptionsJson(dto.getOptionsJson());
        question.setStandardAnswer(dto.getStandardAnswer());
        question.setAnalysis(dto.getAnalysis());
        question.setCreatorId(creatorId);
        question.setIsAiGenerated((byte) 0);
        question.setIsPublic((byte) ("ADMIN".equals(creator != null ? creator.getRole() : null) ? 1 : 0));
        baseMapper.insert(question);
        return question;
    }

    @Override
    @Transactional
    public QuestionBank updateQuestion(Long id, CreateQuestionDTO dto) {
        QuestionBank question = baseMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        question.setCourseCategory(dto.getCourseCategory());
        question.setKnowledgePoint(textOrEmpty(dto.getKnowledgePoint()));
        question.setType(dto.getType());
        question.setDifficulty(dto.getDifficulty());
        question.setContent(dto.getContent());
        question.setOptionsJson(dto.getOptionsJson());
        question.setStandardAnswer(dto.getStandardAnswer());
        question.setAnalysis(dto.getAnalysis());
        baseMapper.updateById(question);
        return question;
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        QuestionBank question = baseMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        baseMapper.deleteById(id);
    }

    @Override
    public IPage<QuestionVO> getQuestionPage(Page<QuestionBank> pageParam, String courseCategory, String type, String difficulty, Long filterCreatorId, Long userId, String userRole) {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        
        if (courseCategory != null && !courseCategory.isEmpty()) {
            wrapper.eq(QuestionBank::getCourseCategory, courseCategory);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(QuestionBank::getType, type);
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            wrapper.eq(QuestionBank::getDifficulty, difficulty);
        }
        if (filterCreatorId != null) {
            wrapper.eq(QuestionBank::getCreatorId, filterCreatorId);
        }
        
        if (!"ADMIN".equals(userRole)) {
            wrapper.and(w -> w.eq(QuestionBank::getIsPublic, (byte) 1)
                    .or()
                    .eq(QuestionBank::getCreatorId, userId));
        }
        
        wrapper.orderByDesc(QuestionBank::getCreateTime);
        IPage<QuestionBank> page = baseMapper.selectPage(pageParam, wrapper);
        
        Page<QuestionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(q -> {
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setCourseCategory(q.getCourseCategory());
            vo.setKnowledgePoint(q.getKnowledgePoint());
            vo.setType(q.getType());
            vo.setDifficulty(q.getDifficulty());
            vo.setContent(q.getContent());
            vo.setOptionsJson(q.getOptionsJson());
            vo.setStandardAnswer(q.getStandardAnswer());
            vo.setAnalysis(q.getAnalysis());
            vo.setCreatorId(q.getCreatorId());
            vo.setIsAiGenerated(q.getIsAiGenerated());
            vo.setIsPublic(q.getIsPublic() != null && q.getIsPublic() == 1);
            vo.setCreateTime(q.getCreateTime());
            
            SysUser creator = sysUserMapper.selectById(q.getCreatorId());
            if (creator != null) {
                vo.setCreatorName(creator.getRealName());
            }
            return vo;
        }).toList());
        
        return voPage;
    }

    @Override
    @Transactional
    public void publishQuestion(Long id) {
        QuestionBank question = baseMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        question.setIsPublic((byte) 1);
        baseMapper.updateById(question);
    }

    private String textOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
