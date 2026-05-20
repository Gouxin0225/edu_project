package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.AiResultActionConfirmDTO;
import com.example.edubackend.dto.AiResultActionConfirmVO;
import com.example.edubackend.dto.AiResultActionPreviewDTO;
import com.example.edubackend.dto.AiResultActionPreviewVO;
import com.example.edubackend.dto.CreateQuestionDTO;
import com.example.edubackend.dto.StudentReviewNoteVO;
import com.example.edubackend.entity.AiChatMessage;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.StudentReviewNote;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AiChatMessageMapper;
import com.example.edubackend.mapper.StudentReviewNoteMapper;
import com.example.edubackend.service.IAiResultActionService;
import com.example.edubackend.service.IQuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiResultActionServiceImpl implements IAiResultActionService {

    private static final int QUESTION_CONTENT_LIMIT = 1200;
    private static final int ANALYSIS_LIMIT = 4000;
    private static final int NOTE_CONTENT_LIMIT = 8000;

    private final AiChatMessageMapper aiChatMessageMapper;
    private final IQuestionBankService questionBankService;
    private final StudentReviewNoteMapper studentReviewNoteMapper;

    @Override
    public AiResultActionPreviewVO preview(AiResultActionPreviewDTO dto, SysUser user) {
        assertActionAllowed(dto.getActionType(), user);
        AiChatMessage sourceMessage = loadSourceMessage(dto.getSourceMessageId(), user, false);
        String sourceContent = resolveSourceContent(dto.getSourceContent(), sourceMessage);
        if (!StringUtils.hasText(sourceContent)) {
            throw new BusinessException(400, "缺少可转换的AI回答内容");
        }

        AiResultActionPreviewVO vo = new AiResultActionPreviewVO();
        vo.setActionType(dto.getActionType());
        vo.setSourceMessageId(sourceMessage == null ? dto.getSourceMessageId() : sourceMessage.getId());

        if (ACTION_CREATE_QUESTION.equals(dto.getActionType())) {
            Map<String, Object> data = buildQuestionPreview(dto, sourceMessage, sourceContent);
            vo.setObjectType("QUESTION");
            vo.setData(data);
            vo.getWarnings().add("预览内容不会自动入库，请教师确认题干、答案和解析后再保存。");
            if (!StringUtils.hasText(textValue(data, "standardAnswer"))) {
                vo.getWarnings().add("未能从AI回答中提取标准答案，保存前必须手动补充。");
            }
            return vo;
        }

        if (ACTION_CREATE_REVIEW_NOTE.equals(dto.getActionType())) {
            vo.setObjectType("REVIEW_NOTE");
            vo.setData(buildReviewNotePreview(dto, sourceMessage, sourceContent));
            vo.getWarnings().add("预览内容仅作为学生个人复习笔记，确认后才会保存。");
            return vo;
        }

        throw new BusinessException(400, "不支持的转换动作");
    }

    @Override
    @Transactional
    public AiResultActionConfirmVO confirm(AiResultActionConfirmDTO dto, SysUser user) {
        assertActionAllowed(dto.getActionType(), user);
        loadSourceMessage(dto.getSourceMessageId(), user, true);

        if (ACTION_CREATE_QUESTION.equals(dto.getActionType())) {
            QuestionBank question = saveQuestion(dto.getData(), user);
            return buildConfirmVO(dto.getActionType(), "QUESTION", question.getId(), "题目已保存到题库");
        }

        if (ACTION_CREATE_REVIEW_NOTE.equals(dto.getActionType())) {
            StudentReviewNote note = saveReviewNote(dto, user);
            return buildConfirmVO(dto.getActionType(), "REVIEW_NOTE", note.getId(), "复习笔记已保存");
        }

        throw new BusinessException(400, "不支持的转换动作");
    }

    @Override
    public List<StudentReviewNoteVO> listMyReviewNotes(SysUser user) {
        if (user == null || !"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可查看个人复习笔记");
        }
        List<StudentReviewNote> notes = studentReviewNoteMapper.selectList(new LambdaQueryWrapper<StudentReviewNote>()
                .eq(StudentReviewNote::getStudentId, user.getId())
                .orderByDesc(StudentReviewNote::getCreateTime));
        return notes.stream().map(this::toNoteVO).toList();
    }

    private Map<String, Object> buildQuestionPreview(AiResultActionPreviewDTO dto, AiChatMessage message, String sourceContent) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseCategory", firstText(dto.getCourseCategory(), message == null ? null : message.getCourseCategory(), "python"));
        data.put("knowledgePoint", firstText(dto.getKnowledgePoint(), message == null ? null : message.getKnowledgePoint(), "综合练习"));
        data.put("type", inferQuestionType(sourceContent));
        data.put("difficulty", "MEDIUM");
        data.put("content", buildQuestionStem(sourceContent));
        data.put("optionsJson", "");
        data.put("standardAnswer", firstText(extractLabeledBlock(sourceContent, List.of("参考答案", "标准答案", "答案")), ""));
        data.put("analysis", truncate(sourceContent, ANALYSIS_LIMIT));
        return data;
    }

    private Map<String, Object> buildReviewNotePreview(AiResultActionPreviewDTO dto, AiChatMessage message, String sourceContent) {
        String knowledgePoint = firstText(dto.getKnowledgePoint(), message == null ? null : message.getKnowledgePoint(), "综合复习");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseCategory", firstText(dto.getCourseCategory(), message == null ? null : message.getCourseCategory(), "python"));
        data.put("knowledgePoint", knowledgePoint);
        data.put("title", buildNoteTitle(knowledgePoint, sourceContent));
        data.put("content", truncate(sourceContent, NOTE_CONTENT_LIMIT));
        data.put("sourceType", "AI_CHAT");
        return data;
    }

    private QuestionBank saveQuestion(Map<String, Object> data, SysUser user) {
        CreateQuestionDTO questionDTO = new CreateQuestionDTO();
        questionDTO.setCourseCategory(requiredText(data, "courseCategory", "课程方向不能为空"));
        questionDTO.setKnowledgePoint(requiredText(data, "knowledgePoint", "知识点不能为空"));
        questionDTO.setType(firstText(textValue(data, "type"), "SHORT"));
        questionDTO.setDifficulty(firstText(textValue(data, "difficulty"), "MEDIUM"));
        questionDTO.setContent(requiredText(data, "content", "题干内容不能为空"));
        questionDTO.setOptionsJson(textValue(data, "optionsJson"));
        questionDTO.setStandardAnswer(requiredText(data, "standardAnswer", "标准答案不能为空"));
        questionDTO.setAnalysis(textValue(data, "analysis"));

        QuestionBank question = questionBankService.createQuestion(questionDTO, user.getId());
        question.setIsAiGenerated((byte) 1);
        questionBankService.updateById(question);
        return question;
    }

    private StudentReviewNote saveReviewNote(AiResultActionConfirmDTO dto, SysUser user) {
        Map<String, Object> data = dto.getData();
        StudentReviewNote note = new StudentReviewNote();
        note.setStudentId(user.getId());
        note.setSourceMessageId(dto.getSourceMessageId());
        note.setCourseCategory(requiredText(data, "courseCategory", "课程方向不能为空"));
        note.setKnowledgePoint(requiredText(data, "knowledgePoint", "知识点不能为空"));
        note.setTitle(requiredText(data, "title", "笔记标题不能为空"));
        note.setContent(requiredText(data, "content", "笔记内容不能为空"));
        note.setSourceType(firstText(textValue(data, "sourceType"), "AI_CHAT"));
        studentReviewNoteMapper.insert(note);
        return note;
    }

    private void assertActionAllowed(String actionType, SysUser user) {
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        String role = user.getRole();
        if (ACTION_CREATE_QUESTION.equals(actionType)) {
            if (!List.of("ADMIN", "TEACHER", "ASSISTANT").contains(role)) {
                throw new BusinessException(403, "仅教师可将AI回答转为题目");
            }
            return;
        }
        if (ACTION_CREATE_REVIEW_NOTE.equals(actionType)) {
            if (!"STUDENT".equals(role)) {
                throw new BusinessException(403, "仅学生可将AI回答加入复习笔记");
            }
            return;
        }
        throw new BusinessException(400, "不支持的转换动作");
    }

    private AiChatMessage loadSourceMessage(Long sourceMessageId, SysUser user, boolean optional) {
        if (sourceMessageId == null) {
            if (optional) {
                return null;
            }
            return null;
        }
        AiChatMessage message = aiChatMessageMapper.selectById(sourceMessageId);
        if (message == null) {
            throw new BusinessException(404, "来源消息不存在");
        }
        if (!message.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "只能转换自己的AI会话消息");
        }
        if (!"ASSISTANT".equals(message.getMessageRole())) {
            throw new BusinessException(400, "仅支持转换AI回答消息");
        }
        return message;
    }

    private String resolveSourceContent(String sourceContent, AiChatMessage sourceMessage) {
        if (StringUtils.hasText(sourceContent)) {
            return sourceContent.trim();
        }
        return sourceMessage == null ? null : sourceMessage.getContent();
    }

    private String buildQuestionStem(String sourceContent) {
        String extracted = extractLabeledBlock(sourceContent, List.of("题目", "题干", "练习"));
        if (StringUtils.hasText(extracted)) {
            return truncate(extracted, QUESTION_CONTENT_LIMIT);
        }
        return "请阅读以下材料并回答问题：\n" + truncate(sourceContent, QUESTION_CONTENT_LIMIT);
    }

    private String inferQuestionType(String sourceContent) {
        String text = sourceContent == null ? "" : sourceContent;
        if (text.contains("A.") || text.contains("A、") || text.contains("选项")) {
            return "SINGLE";
        }
        if (text.contains("代码") || text.contains("编程") || text.contains("实现")) {
            return "CODE";
        }
        return "SHORT";
    }

    private String extractLabeledBlock(String content, List<String> labels) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            for (String label : labels) {
                String prefix = label + ":";
                String chinesePrefix = label + "：";
                if (line.startsWith(prefix) || line.startsWith(chinesePrefix)) {
                    String first = line.substring(line.indexOf(line.startsWith(prefix) ? ":" : "：") + 1).trim();
                    List<String> block = new ArrayList<>();
                    if (StringUtils.hasText(first)) {
                        block.add(first);
                    }
                    for (int j = i + 1; j < lines.length && block.size() < 8; j++) {
                        String next = lines[j].trim();
                        if (looksLikeNextLabel(next)) {
                            break;
                        }
                        if (StringUtils.hasText(next)) {
                            block.add(next);
                        }
                    }
                    return String.join("\n", block);
                }
            }
        }
        return null;
    }

    private boolean looksLikeNextLabel(String line) {
        return line.matches("^(题目|题干|答案|参考答案|标准答案|解析|知识点|难度|题型)[：:].*");
    }

    private String buildNoteTitle(String knowledgePoint, String sourceContent) {
        if (StringUtils.hasText(knowledgePoint)) {
            return truncate(knowledgePoint + "复习笔记", 60);
        }
        for (String line : sourceContent.split("\\R")) {
            if (StringUtils.hasText(line)) {
                return truncate(line.trim(), 60);
            }
        }
        return "AI复习笔记";
    }

    private AiResultActionConfirmVO buildConfirmVO(String actionType, String objectType, Long objectId, String message) {
        AiResultActionConfirmVO vo = new AiResultActionConfirmVO();
        vo.setActionType(actionType);
        vo.setObjectType(objectType);
        vo.setObjectId(objectId);
        vo.setMessage(message);
        return vo;
    }

    private StudentReviewNoteVO toNoteVO(StudentReviewNote note) {
        StudentReviewNoteVO vo = new StudentReviewNoteVO();
        vo.setId(note.getId());
        vo.setStudentId(note.getStudentId());
        vo.setSourceMessageId(note.getSourceMessageId());
        vo.setCourseCategory(note.getCourseCategory());
        vo.setKnowledgePoint(note.getKnowledgePoint());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setSourceType(note.getSourceType());
        vo.setCreateTime(note.getCreateTime());
        vo.setUpdateTime(note.getUpdateTime());
        return vo;
    }

    private String requiredText(Map<String, Object> data, String key, String message) {
        String value = textValue(data, key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private String textValue(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        return value == null ? null : value.toString();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
