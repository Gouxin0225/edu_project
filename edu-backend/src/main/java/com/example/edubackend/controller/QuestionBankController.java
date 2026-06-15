package com.example.edubackend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AiPaperGenerateDTO;
import com.example.edubackend.dto.AiPaperGenerateVO;
import com.example.edubackend.dto.AiGenerateQuestionDTO;
import com.example.edubackend.dto.CreateQuestionDTO;
import com.example.edubackend.dto.ImportQuestionDTO;
import com.example.edubackend.dto.QuestionVO;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.result.Result;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.IAiQuestionService;
import com.example.edubackend.service.IQuestionBankService;
import com.example.edubackend.service.OperationAuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestionBankController {

    private final IQuestionBankService questionBankService;
    private final IAiQuestionService aiQuestionService;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;
    private final OperationAuditLogService auditLogService;

    @GetMapping("/question/creators")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<Map<String, Object>>> getCreators() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getRole, Arrays.asList("ADMIN", "TEACHER", "ASSISTANT"))
                .eq(SysUser::getIsDeleted, (byte) 0);
        wrapper.orderByAsc(SysUser::getRealName);
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        List<Map<String, Object>> result = users.stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "realName", u.getRealName(), "role", u.getRole()))
                .toList();
        return Result.success(result);
    }

    @PostMapping("/question")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<QuestionVO> createQuestion(@Valid @RequestBody CreateQuestionDTO dto) {
        Long userId = UserContext.getUserId();
        QuestionBank question = questionBankService.createQuestion(dto, userId);
        auditLogService.record("QUESTION_CREATE", "QUESTION", question.getId(),
                "创建题目 " + question.getContent());
        return Result.success(toVO(question));
    }

    @PutMapping("/question/{id}")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<QuestionVO> updateQuestion(@PathVariable Long id, @Valid @RequestBody CreateQuestionDTO dto) {
        SysUser user = UserContext.getUser();
        QuestionBank existing = questionBankService.getById(id);
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }
        if (!"ADMIN".equals(user.getRole()) && !existing.getCreatorId().equals(user.getId())) {
            throw new BusinessException(403, "只能编辑自己的题目");
        }
        QuestionBank question = questionBankService.updateQuestion(id, dto);
        return Result.success(toVO(question));
    }

    @DeleteMapping("/question/{id}")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        SysUser user = UserContext.getUser();
        QuestionBank existing = questionBankService.getById(id);
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }
        if (!"ADMIN".equals(user.getRole()) && !existing.getCreatorId().equals(user.getId())) {
            throw new BusinessException(403, "只能删除自己的题目");
        }
        questionBankService.deleteQuestion(id);
        auditLogService.record("QUESTION_DELETE", "QUESTION", id,
                "删除题目 " + existing.getContent());
        return Result.success();
    }

    @GetMapping("/question")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<IPage<QuestionVO>> getQuestionPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String courseCategory,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Long creatorId) {
        SysUser user = UserContext.getUser();
        Page<QuestionBank> pageParam = new Page<>(page, size);
        IPage<QuestionVO> result = questionBankService.getQuestionPage(
                pageParam, courseCategory, type, difficulty, creatorId, user.getId(), user.getRole());
        return Result.success(result);
    }

    @PostMapping("/question/import")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file) throws IOException {
        List<ImportQuestionDTO> questions = EasyExcel.read(file.getInputStream())
                .head(ImportQuestionDTO.class)
                .sheet()
                .doReadSync();

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        Long userId = UserContext.getUserId();

        for (int i = 0; i < questions.size(); i++) {
            ImportQuestionDTO dto = questions.get(i);
            int rowNum = i + 2;
            try {
                CreateQuestionDTO createDTO = new CreateQuestionDTO();
                createDTO.setCourseCategory(dto.getCourseCategory());
                createDTO.setKnowledgePoint(dto.getKnowledgePoint());
                createDTO.setType(dto.getType());
                createDTO.setDifficulty(dto.getDifficulty());
                createDTO.setContent(firstText(dto.getContent(), dto.getLegacyContent()));
                createDTO.setOptionsJson(buildOptionsJson(dto));
                createDTO.setStandardAnswer(firstText(dto.getStandardAnswer(), dto.getLegacyStandardAnswer()));
                createDTO.setAnalysis(dto.getAnalysis());
                
                questionBankService.createQuestion(createDTO, userId);
                successCount++;
            } catch (Exception e) {
                errors.add("第" + rowNum + "行: " + e.getMessage());
            }
        }

        log.info("用户{}批量导入题目: 成功{}条, 失败{}条", userId, successCount, errors.size());
        return Result.success(Map.of("success", successCount, "failed", errors.size(), "errors", errors));
    }

    @PostMapping("/question/ai-generate")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Map<String, Object>> aiGenerateQuestions(@Valid @RequestBody AiGenerateQuestionDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("用户{}请求AI生成题目: 知识点={}, 题型={}, 难度={}, 数量={}", 
                userId, dto.getKnowledgePoint(), dto.getTypes(), dto.getDifficulty(), dto.getCount());
        
        List<QuestionBank> generatedQuestions = aiQuestionService.generateQuestions(dto, userId);
        
        for (QuestionBank question : generatedQuestions) {
            questionBankService.save(question);
        }
        
        log.info("AI生成题目成功, 共{}道", generatedQuestions.size());
        return Result.success(Map.of(
                "message", "生成成功",
                "count", generatedQuestions.size()
        ));
    }

    @PostMapping("/question/ai-paper")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<AiPaperGenerateVO> aiGeneratePaper(@Valid @RequestBody AiPaperGenerateDTO dto) {
        SysUser user = UserContext.getUser();
        Long userId = user.getId();
        int requestedCount = dto.getCount() == null ? 10 : dto.getCount();

        List<QuestionBank> selected = new ArrayList<>();
        if (Boolean.TRUE.equals(dto.getPreferExisting())) {
            selected.addAll(pickExistingQuestions(dto, user, requestedCount));
        }

        int existingCount = selected.size();
        int missingCount = Math.max(0, requestedCount - selected.size());
        List<QuestionBank> generated = new ArrayList<>();
        if (missingCount > 0) {
            AiGenerateQuestionDTO aiDto = new AiGenerateQuestionDTO();
            aiDto.setCourseCategory(dto.getCourseCategory());
            aiDto.setKnowledgePoint(dto.getKnowledgePoint());
            aiDto.setContext(dto.getContext());
            aiDto.setTypes(dto.getTypes());
            aiDto.setDifficulty(dto.getDifficulty());
            aiDto.setCount(missingCount);

            generated = aiQuestionService.generateQuestions(aiDto, userId);
            if (generated.size() > missingCount) {
                generated = new ArrayList<>(generated.subList(0, missingCount));
            }
            for (QuestionBank question : generated) {
                questionBankService.save(question);
            }
            selected.addAll(generated);
        }

        AiPaperGenerateVO vo = new AiPaperGenerateVO();
        vo.setRequestedCount(requestedCount);
        vo.setExistingCount(existingCount);
        vo.setAiGeneratedCount(generated.size());
        vo.setQuestions(selected.stream().map(this::toVO).toList());
        vo.setMessage(String.format("已组卷%d题：题库抽取%d题，AI补齐%d题", selected.size(), existingCount, generated.size()));
        log.info("用户{}AI组卷完成: 请求{}题, 题库{}题, AI{}题", userId, requestedCount, existingCount, generated.size());
        return Result.success(vo);
    }

    @PostMapping("/admin/question/{id}/publish")
    @RequireRole("ADMIN")
    public Result<Void> publishQuestion(@PathVariable Long id) {
        questionBankService.publishQuestion(id);
        return Result.success("题目已公开");
    }

    private QuestionVO toVO(QuestionBank question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setCourseCategory(question.getCourseCategory());
        vo.setKnowledgePoint(question.getKnowledgePoint());
        vo.setType(question.getType());
        vo.setDifficulty(question.getDifficulty());
        vo.setContent(question.getContent());
        vo.setOptionsJson(question.getOptionsJson());
        vo.setStandardAnswer(question.getStandardAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setCreatorId(question.getCreatorId());
        vo.setIsAiGenerated(question.getIsAiGenerated());
        vo.setIsPublic(question.getIsPublic() != null && question.getIsPublic() == 1);
        vo.setCreateTime(question.getCreateTime());
        return vo;
    }

    private List<QuestionBank> pickExistingQuestions(AiPaperGenerateDTO dto, SysUser user, int limit) {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBank::getCourseCategory, dto.getCourseCategory());
        wrapper.in(QuestionBank::getType, dto.getTypes());
        wrapper.eq(QuestionBank::getDifficulty, dto.getDifficulty());
        if (StringUtils.hasText(dto.getKnowledgePoint())) {
            wrapper.like(QuestionBank::getKnowledgePoint, dto.getKnowledgePoint().trim());
        }

        Set<Long> excludedIds = dto.getExcludeQuestionIds() == null
                ? Set.of()
                : new HashSet<>(dto.getExcludeQuestionIds());
        if (!excludedIds.isEmpty()) {
            wrapper.notIn(QuestionBank::getId, excludedIds);
        }

        if (!"ADMIN".equals(user.getRole())) {
            wrapper.and(w -> w.eq(QuestionBank::getIsPublic, (byte) 1)
                    .or()
                    .eq(QuestionBank::getCreatorId, user.getId()));
        }

        List<QuestionBank> available = questionBankService.list(wrapper);
        Collections.shuffle(available);
        return available.size() <= limit ? available : new ArrayList<>(available.subList(0, limit));
    }

    private CreateQuestionDTO toCreateDTO(QuestionBank question) {
        CreateQuestionDTO dto = new CreateQuestionDTO();
        dto.setCourseCategory(question.getCourseCategory());
        dto.setKnowledgePoint(question.getKnowledgePoint());
        dto.setType(question.getType());
        dto.setDifficulty(question.getDifficulty());
        dto.setContent(question.getContent());
        dto.setOptionsJson(question.getOptionsJson());
        dto.setStandardAnswer(question.getStandardAnswer());
        dto.setAnalysis(question.getAnalysis());
        return dto;
    }

    private String buildOptionsJson(ImportQuestionDTO dto) throws IOException {
        if (StringUtils.hasText(dto.getOptionsJson())) {
            return dto.getOptionsJson();
        }
        if (!"SINGLE".equals(dto.getType()) && !"MULTIPLE".equals(dto.getType())) {
            return null;
        }
        List<String> options = new ArrayList<>();
        for (String option : List.of(dto.getOptionA(), dto.getOptionB(), dto.getOptionC(), dto.getOptionD())) {
            if (StringUtils.hasText(option)) {
                options.add(option.trim());
            }
        }
        return options.isEmpty() ? null : objectMapper.writeValueAsString(options);
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
