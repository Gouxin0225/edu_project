package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.AiGenerateQuestionDTO;
import com.example.edubackend.dto.CreateExamDTO;
import com.example.edubackend.dto.CreateTemplateDTO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.dto.OutlineConfirmDTO;
import com.example.edubackend.dto.OutlineConfirmVO;
import com.example.edubackend.dto.OutlineDocumentVO;
import com.example.edubackend.dto.OutlineQuestionUpdateDTO;
import com.example.edubackend.dto.OutlineQuestionVO;
import com.example.edubackend.dto.TemplateVO;
import com.example.edubackend.entity.OutlineDocument;
import com.example.edubackend.entity.OutlineGeneratedQuestion;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.OutlineDocumentMapper;
import com.example.edubackend.mapper.OutlineGeneratedQuestionMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.IAiOutlineExamService;
import com.example.edubackend.service.IAiQuestionService;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.IQuestionPaperTemplateService;
import com.example.edubackend.service.IQuestionBankService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOutlineExamServiceImpl implements IAiOutlineExamService {

    private final OutlineDocumentMapper documentMapper;
    private final OutlineGeneratedQuestionMapper generatedQuestionMapper;
    private final IQuestionBankService questionBankService;
    private final IAiQuestionService aiQuestionService;
    private final IAssessmentTaskService assessmentTaskService;
    private final IQuestionPaperTemplateService templateService;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final int DEFAULT_QUESTION_COUNT = 20;
    private static final int MAX_DIRECT_QUESTION_COUNT = 50;
    private static final Set<String> CHOICE_TYPES = Set.of("SINGLE", "MULTIPLE");

    @Value("${file.upload-dir:/edu-platform/uploads}")
    private String uploadDir;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.endpoint:https://api.deepseek.com/v1/chat/completions}")
    private String apiEndpoint;

    @Value("${ai.api.model:deepseek-chat}")
    private String model;

    @Override
    @Transactional
    public OutlineDocumentVO upload(MultipartFile file, String title, String courseCategory, Long creatorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传大纲文件");
        }
        if (!StringUtils.hasText(courseCategory)) {
            throw new BusinessException(400, "课程方向不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "outline.txt";
        String text = extractText(file, filename);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(400, "未能从大纲文件中解析出文本内容");
        }

        try {
            Path uploadPath = Paths.get(uploadDir, "outlines");
            Files.createDirectories(uploadPath);
            String storedName = UUID.randomUUID() + "_" + filename.replaceAll("[\\\\/]", "_");
            Path target = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), target);

            OutlineDocument document = new OutlineDocument();
            document.setTitle(StringUtils.hasText(title) ? title.trim() : stripExtension(filename));
            document.setOriginalFilename(filename);
            document.setFilePath(target.toString());
            document.setCourseCategory(courseCategory.trim());
            document.setOutlineText(text.trim());
            document.setStatus("UPLOADED");
            document.setCreatorId(creatorId);
            document.setIsDeleted((byte) 0);
            documentMapper.insert(document);
            return toDocumentVO(document);
        } catch (IOException e) {
            throw new BusinessException(500, "保存大纲文件失败: " + e.getMessage());
        }
    }

    @Override
    public OutlineDocumentVO getDocument(Long id, Long userId, String role) {
        return toDocumentVO(loadDocument(id, userId, role));
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, Long userId, String role) {
        loadDocument(id, userId, role);
        generatedQuestionMapper.delete(new LambdaQueryWrapper<OutlineGeneratedQuestion>()
                .eq(OutlineGeneratedQuestion::getDocumentId, id));
        documentMapper.deleteById(id);
    }

    @Override
    @Transactional
    public List<OutlineQuestionVO> generateQuestions(Long id, Long userId, String role) {
        OutlineDocument document = loadDocument(id, userId, role);

        generatedQuestionMapper.delete(new LambdaQueryWrapper<OutlineGeneratedQuestion>()
                .eq(OutlineGeneratedQuestion::getDocumentId, id)
                .isNull(OutlineGeneratedQuestion::getQuestionId));

        List<QuestionBank> questions = buildQuestionsFromOutline(document, DEFAULT_QUESTION_COUNT, userId);
        List<OutlineQuestionVO> result = new ArrayList<>();
        for (QuestionBank question : questions) {
            OutlineGeneratedQuestion staged = toStagedQuestion(document, question);
            generatedQuestionMapper.insert(staged);
            result.add(toQuestionVO(staged));
        }
        document.setStatus("GENERATED");
        documentMapper.updateById(document);
        return result;
    }

    @Override
    public List<OutlineQuestionVO> listQuestions(Long id, Long userId, String role) {
        loadDocument(id, userId, role);
        return generatedQuestionMapper.selectList(new LambdaQueryWrapper<OutlineGeneratedQuestion>()
                        .eq(OutlineGeneratedQuestion::getDocumentId, id)
                        .orderByAsc(OutlineGeneratedQuestion::getId))
                .stream()
                .map(this::toQuestionVO)
                .toList();
    }

    @Override
    @Transactional
    public OutlineQuestionVO updateQuestion(Long questionId, OutlineQuestionUpdateDTO dto, Long userId, String role) {
        OutlineGeneratedQuestion question = generatedQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "待确认题目不存在");
        }
        loadDocument(question.getDocumentId(), userId, role);
        if (dto.getSelected() != null) question.setSelected((byte) (dto.getSelected() ? 1 : 0));
        if (StringUtils.hasText(dto.getKnowledgePoint())) question.setKnowledgePoint(dto.getKnowledgePoint().trim());
        if (StringUtils.hasText(dto.getType())) question.setType(dto.getType().trim());
        if (StringUtils.hasText(dto.getDifficulty())) question.setDifficulty(dto.getDifficulty().trim());
        if (StringUtils.hasText(dto.getContent())) question.setContent(dto.getContent().trim());
        if (dto.getOptionsJson() != null) question.setOptionsJson(trimToNull(dto.getOptionsJson()));
        if (StringUtils.hasText(dto.getStandardAnswer())) question.setStandardAnswer(dto.getStandardAnswer().trim());
        if (dto.getAnalysis() != null) question.setAnalysis(trimToNull(dto.getAnalysis()));
        generatedQuestionMapper.updateById(question);
        return toQuestionVO(question);
    }

    @Override
    @Transactional
    public OutlineConfirmVO confirm(Long id, OutlineConfirmDTO dto, Long userId, String role) {
        OutlineDocument document = loadDocument(id, userId, role);
        List<OutlineGeneratedQuestion> stagedQuestions = generatedQuestionMapper.selectList(
                new LambdaQueryWrapper<OutlineGeneratedQuestion>()
                        .eq(OutlineGeneratedQuestion::getDocumentId, id)
                        .eq(OutlineGeneratedQuestion::getSelected, (byte) 1)
                        .orderByAsc(OutlineGeneratedQuestion::getId)
        );
        if (stagedQuestions.isEmpty()) {
            throw new BusinessException(400, "没有可确认的题目");
        }

        List<Long> questionIds = new ArrayList<>();
        for (OutlineGeneratedQuestion staged : stagedQuestions) {
            Long questionId = staged.getQuestionId();
            if (questionId == null) {
                QuestionBank question = toQuestionBank(staged, userId);
                questionBankService.save(question);
                staged.setQuestionId(question.getId());
                generatedQuestionMapper.updateById(staged);
                questionId = question.getId();
            }
            questionIds.add(questionId);
        }

        Long examId = null;
        Long templateId = null;
        if (dto != null && Boolean.TRUE.equals(dto.getCreateTemplate())) {
            CreateTemplateDTO templateDTO = new CreateTemplateDTO();
            templateDTO.setName(StringUtils.hasText(dto.getTemplateName()) ? dto.getTemplateName().trim() : document.getTitle() + " - AI大纲模板");
            templateDTO.setDescription("由AI大纲出卷生成，来源文件：" + document.getOriginalFilename());
            templateDTO.setCourseName(document.getCourseCategory());
            templateDTO.setQuestionIds(questionIds);
            templateDTO.setScores(distributeScores(questionIds.size(), 100));
            templateDTO.setTotalScore(100);
            TemplateVO template = templateService.createTemplate(templateDTO, userId, getCreatorName(userId));
            templateId = template.getId();
        }

        if (dto != null && Boolean.TRUE.equals(dto.getCreateExam())) {
            if (dto.getEndTime() == null) {
                throw new BusinessException(400, "创建考试时截止时间不能为空");
            }
            CreateExamDTO examDTO = new CreateExamDTO();
            examDTO.setTitle(StringUtils.hasText(dto.getExamTitle()) ? dto.getExamTitle().trim() : document.getTitle() + " - AI大纲试卷");
            examDTO.setStartTime(dto.getStartTime());
            examDTO.setEndTime(dto.getEndTime());
            examDTO.setTargetClassIds(dto.getTargetClassIds());
            examDTO.setType("EXAM");
            ExamVO exam = assessmentTaskService.createExam(examDTO, userId);
            examId = exam.getId();
            assessmentTaskService.addQuestions(examId, questionIds);
            assessmentTaskService.autoScore(examId);
        }

        document.setStatus(resolveConfirmStatus(examId, templateId));
        documentMapper.updateById(document);

        OutlineConfirmVO vo = new OutlineConfirmVO();
        vo.setSavedCount(questionIds.size());
        vo.setQuestionIds(questionIds);
        vo.setExamId(examId);
        vo.setTemplateId(templateId);
        return vo;
    }

    private List<QuestionBank> buildQuestionsFromOutline(OutlineDocument document, int targetCount, Long creatorId) {
        int count = Math.max(1, Math.min(targetCount, MAX_DIRECT_QUESTION_COUNT));
        try {
            return callQuestionBankAiForOutlineQuestions(document, count, creatorId);
        } catch (Exception e) {
            log.warn("调用题库AI出题程序失败，使用本地规则兜底: {}", e.getMessage());
        }
        return buildFallbackOutlineQuestions(document, count);
    }

    private List<QuestionBank> callQuestionBankAiForOutlineQuestions(OutlineDocument document, int targetCount, Long creatorId) {
        List<QuestionBank> questions = new ArrayList<>();
        List<String> keyPoints = extractKeyPoints(document.getOutlineText(), 12);
        Map<String, Integer> difficultyCounts = conventionalDifficultyCounts(targetCount);
        int batchIndex = 0;
        for (Map.Entry<String, Integer> entry : difficultyCounts.entrySet()) {
            int batchCount = entry.getValue();
            if (batchCount <= 0) {
                continue;
            }
            AiGenerateQuestionDTO dto = new AiGenerateQuestionDTO();
            dto.setCourseCategory(document.getCourseCategory());
            dto.setKnowledgePoint(buildBatchKnowledgePoint(keyPoints, batchIndex));
            dto.setContext(buildOutlineContext(document));
            dto.setTypes(conventionalTypeSequence(batchCount, document.getCourseCategory()));
            dto.setDifficulty(entry.getKey());
            dto.setCount(batchCount);
            questions.addAll(aiQuestionService.generateQuestions(dto, creatorId));
            batchIndex++;
        }
        if (questions.isEmpty()) {
            throw new BusinessException(500, "题库AI出题程序未生成可用题目");
        }
        if (questions.size() > targetCount) {
            return new ArrayList<>(questions.subList(0, targetCount));
        }
        return questions;
    }

    private List<QuestionBank> callAiForOutlineQuestions(OutlineDocument document, int targetCount) throws Exception {
        String outline = document.getOutlineText();
        if (outline.length() > 16000) {
            outline = outline.substring(0, 16000);
        }
        String systemPrompt = """
                你是教学考试命题专家。请直接根据上传的课程大纲分析重点、难点和考试覆盖面，并生成一套待讲师审核的考试题目。
                只输出JSON数组，不要输出解释或Markdown。
                每个对象必须包含：
                - type: SINGLE、MULTIPLE、JUDGE、SHORT、CODE之一
                - difficulty: EASY、MEDIUM、HARD之一
                - knowledge_point: 命中的大纲知识点
                - content: 题干
                - options_json: SINGLE/MULTIPLE为字符串数组，其他题型为null
                - standard_answer: 标准答案。单选用A/B/C/D，多选用AB/AC等，判断用true/false
                - analysis: 简明解析，说明对应的大纲重点或难点

                命题要求：
                1. 先识别大纲中的核心重点、难点、易错点，再围绕这些内容出题。
                2. 题型占比按常规考试：客观题为主，单选约35%，多选约20%，判断约15%，简答约20%，编程/实操约10%；若课程不适合编程/实操，可把该比例转为简答或案例分析。
                3. 难度占比按常规考试：简单约30%，中等约50%，困难约20%。
                4. 题目不得脱离大纲，不要重复考查同一个表述。
                5. 选择题至少4个选项，干扰项要合理。
                """;
        String userPrompt = """
                课程方向：%s
                题目数量：%d

                课程大纲：
                %s
                """.formatted(document.getCourseCategory(), targetCount, outline);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.35);
        body.put("max_tokens", 8192);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        ResponseEntity<String> response = restTemplate.exchange(apiEndpoint, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(500, "AI API返回为空");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = String.valueOf(message.get("content"));
        List<Map<String, Object>> rawQuestions = objectMapper.readValue(extractJsonArray(content), new TypeReference<List<Map<String, Object>>>() {});
        return normalizeOutlineQuestions(rawQuestions, document, targetCount);
    }

    private List<QuestionBank> normalizeOutlineQuestions(List<Map<String, Object>> rawQuestions, OutlineDocument document, int targetCount) {
        List<QuestionBank> questions = new ArrayList<>();
        if (rawQuestions == null) {
            return questions;
        }
        List<String> fallbackTypes = conventionalTypeSequence(targetCount, document.getCourseCategory());
        for (int i = 0; i < rawQuestions.size() && questions.size() < targetCount; i++) {
            Map<String, Object> data = rawQuestions.get(i);
            String type = normalizeType(firstText(data, "type", "question_type", "题型"));
            if (!StringUtils.hasText(type)) {
                type = fallbackTypes.get(i % fallbackTypes.size());
            }
            String difficulty = normalizeDifficulty(firstText(data, "difficulty", "level", "难度"));
            String content = firstText(data, "content", "question", "title", "题干");
            String answer = firstText(data, "standard_answer", "standardAnswer", "answer", "correct_answer", "correctAnswer", "标准答案", "参考答案");
            String analysis = firstText(data, "analysis", "explanation", "解析");
            List<String> options = normalizeOptions(type, firstValue(data, "options_json", "options", "choices", "选项"));
            answer = normalizeAnswer(type, answer, options, analysis);
            if (!StringUtils.hasText(content) || !StringUtils.hasText(answer)) {
                continue;
            }
            if (CHOICE_TYPES.contains(type) && (options == null || options.size() < 2)) {
                continue;
            }

            QuestionBank question = new QuestionBank();
            question.setCourseCategory(document.getCourseCategory());
            question.setKnowledgePoint(textOrEmpty(firstText(data, "knowledge_point", "knowledgePoint", "知识点"), inferKnowledgePoint(content, document.getOutlineText())));
            question.setType(type);
            question.setDifficulty(difficulty);
            question.setContent(content.trim());
            question.setOptionsJson(toOptionsJson(type, options));
            question.setStandardAnswer(answer);
            question.setAnalysis(trimToNull(analysis));
            question.setIsAiGenerated((byte) 1);
            question.setIsPublic((byte) 1);
            questions.add(question);
        }
        if (questions.isEmpty()) {
            throw new BusinessException(500, "AI未能生成可用题目");
        }
        return questions;
    }

    private List<QuestionBank> buildFallbackOutlineQuestions(OutlineDocument document, int targetCount) {
        List<String> points = extractKeyPoints(document.getOutlineText(), Math.min(10, targetCount));
        List<String> types = conventionalTypeSequence(targetCount, document.getCourseCategory());
        List<QuestionBank> questions = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) {
            String point = points.get(i % points.size());
            String type = types.get(i % types.size());
            QuestionBank question = new QuestionBank();
            question.setCourseCategory(document.getCourseCategory());
            question.setKnowledgePoint(point);
            question.setType(type);
            question.setDifficulty(i % 10 < 3 ? "EASY" : (i % 10 < 8 ? "MEDIUM" : "HARD"));
            question.setContent(buildFallbackContent(type, point));
            if (CHOICE_TYPES.contains(type)) {
                question.setOptionsJson("[\"概念定义和适用场景\",\"命令格式或语法细节\",\"常见错误与排查方法\",\"综合实践应用\"]");
                question.setStandardAnswer("A");
            } else if ("JUDGE".equals(type)) {
                question.setStandardAnswer("true");
            } else {
                question.setStandardAnswer("围绕大纲中该知识点的核心概念、操作步骤、常见问题和应用场景作答。");
            }
            question.setAnalysis("该题来自大纲知识点：" + point + "。请讲师审核后保留或编辑。");
            question.setIsAiGenerated((byte) 1);
            question.setIsPublic((byte) 1);
            questions.add(question);
        }
        return questions;
    }

    private String buildFallbackContent(String type, String point) {
        return switch (type) {
            case "SINGLE" -> "关于“" + point + "”，下列哪一项最符合课程大纲要求？";
            case "MULTIPLE" -> "围绕“" + point + "”，哪些内容通常属于考试重点？";
            case "JUDGE" -> "“" + point + "”是本课程大纲中的重点考查内容之一。";
            case "CODE" -> "请结合“" + point + "”设计一个实操或编程任务，并说明关键步骤。";
            default -> "请简述“" + point + "”的核心内容、难点和典型应用。";
        };
    }

    private List<String> conventionalTypeSequence(int targetCount, String courseCategory) {
        boolean practicalCourse = isPracticalCourse(courseCategory);
        int single = Math.max(1, Math.round(targetCount * 0.35f));
        int multiple = Math.max(1, Math.round(targetCount * 0.20f));
        int judge = Math.max(1, Math.round(targetCount * 0.15f));
        int code = practicalCourse && targetCount >= 8 ? Math.max(1, Math.round(targetCount * 0.10f)) : 0;
        int shortCount = Math.max(1, targetCount - single - multiple - judge - code);
        List<String> types = new ArrayList<>();
        addRepeated(types, "SINGLE", single);
        addRepeated(types, "MULTIPLE", multiple);
        addRepeated(types, "JUDGE", judge);
        addRepeated(types, "SHORT", shortCount);
        addRepeated(types, "CODE", code);
        while (types.size() > targetCount) {
            types.remove(types.size() - 1);
        }
        while (types.size() < targetCount) {
            types.add("SHORT");
        }
        return types;
    }

    private Map<String, Integer> conventionalDifficultyCounts(int targetCount) {
        int easy = Math.max(1, Math.round(targetCount * 0.30f));
        int hard = Math.max(1, Math.round(targetCount * 0.20f));
        int medium = Math.max(1, targetCount - easy - hard);
        while (easy + medium + hard > targetCount) {
            if (medium > 1) {
                medium--;
            } else if (easy > 1) {
                easy--;
            } else {
                hard--;
            }
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("EASY", easy);
        counts.put("MEDIUM", medium);
        counts.put("HARD", hard);
        return counts;
    }

    private String buildBatchKnowledgePoint(List<String> keyPoints, int batchIndex) {
        if (keyPoints == null || keyPoints.isEmpty()) {
            return "课程核心知识";
        }
        int limit = Math.min(keyPoints.size(), 5);
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            selected.add(keyPoints.get((batchIndex * limit + i) % keyPoints.size()));
        }
        return truncate(String.join("、", selected), 90);
    }

    private String buildOutlineContext(OutlineDocument document) {
        String outline = document.getOutlineText();
        if (outline.length() > 12000) {
            outline = outline.substring(0, 12000);
        }
        return """
                请严格依据以下课程大纲出题，题目要覆盖核心重点、难点、易错点，避免泛泛而谈。
                大纲标题：%s
                课程方向：%s

                大纲内容：
                %s
                """.formatted(document.getTitle(), document.getCourseCategory(), outline);
    }

    private List<Integer> distributeScores(int questionCount, int totalScore) {
        List<Integer> scores = new ArrayList<>();
        if (questionCount <= 0) {
            return scores;
        }
        int base = totalScore / questionCount;
        int remainder = totalScore % questionCount;
        for (int i = 0; i < questionCount; i++) {
            scores.add(base + (i < remainder ? 1 : 0));
        }
        return scores;
    }

    private String getCreatorName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return "用户" + userId;
    }

    private String resolveConfirmStatus(Long examId, Long templateId) {
        if (examId != null && templateId != null) {
            return "EXAM_AND_TEMPLATE_CREATED";
        }
        if (examId != null) {
            return "EXAM_CREATED";
        }
        if (templateId != null) {
            return "TEMPLATE_CREATED";
        }
        return "CONFIRMED";
    }

    private boolean isPracticalCourse(String courseCategory) {
        String value = courseCategory == null ? "" : courseCategory.toLowerCase(Locale.ROOT);
        return value.contains("python") || value.contains("web") || value.contains("mysql") || value.contains("rhce") || value.contains("csa");
    }

    private void addRepeated(List<String> target, String value, int count) {
        for (int i = 0; i < count; i++) {
            target.add(value);
        }
    }

    private List<String> extractKeyPoints(String outlineText, int limit) {
        List<String> points = new ArrayList<>();
        for (String line : outlineText.split("\\R")) {
            String cleaned = line.replaceFirst("^\\s*[#\\-*\\d.、）)]+\\s*", "").trim();
            cleaned = cleaned.replaceAll("\\s+", " ");
            if (cleaned.length() >= 2 && cleaned.length() <= 80 && !points.contains(cleaned)) {
                points.add(cleaned);
            }
            if (points.size() >= limit) break;
        }
        if (points.isEmpty()) {
            points.add("课程核心知识");
        }
        return points;
    }

    private OutlineDocument loadDocument(Long id, Long userId, String role) {
        OutlineDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(404, "大纲文档不存在");
        }
        if (!"ADMIN".equals(role) && !document.getCreatorId().equals(userId)) {
            throw new BusinessException(403, "只能操作自己上传的大纲");
        }
        return document;
    }

    private String extractText(MultipartFile file, String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".txt") || lower.endsWith(".md")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            if (lower.endsWith(".docx")) {
                return extractDocxText(file.getBytes());
            }
        } catch (IOException e) {
            throw new BusinessException(400, "读取大纲文件失败: " + e.getMessage());
        }
        throw new BusinessException(400, "当前支持 .txt、.md、.docx 大纲文件");
    }

    private String extractDocxText(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return xml.replaceAll("</w:p>", "\n")
                            .replaceAll("<[^>]+>", "")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&amp;", "&")
                            .trim();
                }
            }
        }
        return "";
    }

    private OutlineGeneratedQuestion toStagedQuestion(OutlineDocument document, QuestionBank question) {
        OutlineGeneratedQuestion staged = new OutlineGeneratedQuestion();
        staged.setDocumentId(document.getId());
        staged.setCourseCategory(document.getCourseCategory());
        staged.setKnowledgePoint(truncate(textOrEmpty(question.getKnowledgePoint(), ""), 100));
        staged.setType(question.getType());
        staged.setDifficulty(question.getDifficulty());
        staged.setContent(question.getContent());
        staged.setOptionsJson(question.getOptionsJson());
        staged.setStandardAnswer(question.getStandardAnswer());
        staged.setAnalysis(question.getAnalysis());
        staged.setSelected((byte) 1);
        return staged;
    }

    private QuestionBank toQuestionBank(OutlineGeneratedQuestion staged, Long creatorId) {
        QuestionBank question = new QuestionBank();
        question.setCourseCategory(staged.getCourseCategory());
        question.setKnowledgePoint(truncate(textOrEmpty(staged.getKnowledgePoint(), ""), 100));
        question.setType(staged.getType());
        question.setDifficulty(staged.getDifficulty());
        question.setContent(staged.getContent());
        question.setOptionsJson(staged.getOptionsJson());
        question.setStandardAnswer(staged.getStandardAnswer());
        question.setAnalysis(staged.getAnalysis());
        question.setCreatorId(creatorId);
        question.setIsAiGenerated((byte) 1);
        question.setIsPublic((byte) 1);
        return question;
    }

    private OutlineDocumentVO toDocumentVO(OutlineDocument document) {
        OutlineDocumentVO vo = new OutlineDocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setCourseCategory(document.getCourseCategory());
        vo.setOutlineText(document.getOutlineText());
        vo.setStatus(document.getStatus());
        vo.setCreateTime(document.getCreateTime());
        return vo;
    }

    private OutlineQuestionVO toQuestionVO(OutlineGeneratedQuestion question) {
        OutlineQuestionVO vo = new OutlineQuestionVO();
        vo.setId(question.getId());
        vo.setDocumentId(question.getDocumentId());
        vo.setCourseCategory(question.getCourseCategory());
        vo.setKnowledgePoint(question.getKnowledgePoint());
        vo.setType(question.getType());
        vo.setDifficulty(question.getDifficulty());
        vo.setContent(question.getContent());
        vo.setOptionsJson(question.getOptionsJson());
        vo.setStandardAnswer(question.getStandardAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setSelected(question.getSelected() != null && question.getSelected() == 1);
        vo.setQuestionId(question.getQuestionId());
        return vo;
    }

    private List<String> normalizeOptions(String type, Object rawOptions) {
        if (!CHOICE_TYPES.contains(type) || rawOptions == null) {
            return null;
        }
        List<String> options = new ArrayList<>();
        if (rawOptions instanceof List<?> list) {
            for (Object item : list) {
                String option = normalizeOptionText(item);
                if (StringUtils.hasText(option)) {
                    options.add(option);
                }
            }
        } else if (rawOptions instanceof String text) {
            options.addAll(parseOptionsText(text));
        } else if (rawOptions instanceof Map<?, ?> map) {
            map.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .map(this::normalizeOptionText)
                    .filter(StringUtils::hasText)
                    .forEach(options::add);
        }
        return options;
    }

    private String normalizeOptionText(Object item) {
        if (item == null) {
            return "";
        }
        if (item instanceof Map<?, ?> map) {
            Object value = firstMapValue(map, "content", "text", "value", "label", "option");
            return value == null ? "" : stripOptionPrefix(String.valueOf(value));
        }
        return stripOptionPrefix(String.valueOf(item));
    }

    private List<String> parseOptionsText(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }
        String trimmed = text.trim();
        try {
            return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {})
                    .stream()
                    .map(this::stripOptionPrefix)
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception ignored) {
            // 继续按普通文本解析。
        }
        List<String> options = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?i)(?:^|[\\n;；])\\s*[A-H][\\.、．:]\\s*([^\\n;；]+)").matcher(trimmed);
        while (matcher.find()) {
            options.add(stripOptionPrefix(matcher.group(1)));
        }
        if (options.isEmpty()) {
            for (String part : trimmed.split("[\\n;；|]")) {
                String option = stripOptionPrefix(part);
                if (StringUtils.hasText(option)) {
                    options.add(option);
                }
            }
        }
        return options;
    }

    private String normalizeAnswer(String type, String rawAnswer, List<String> options, String analysis) {
        String answer = StringUtils.hasText(rawAnswer) ? rawAnswer.trim() : extractAnswerFromText(analysis);
        if ("JUDGE".equals(type)) {
            return normalizeJudgeAnswer(answer);
        }
        if ("SINGLE".equals(type)) {
            String letters = extractChoiceLetters(answer, options == null ? 0 : options.size());
            return StringUtils.hasText(letters) ? letters.substring(0, 1) : "";
        }
        if ("MULTIPLE".equals(type)) {
            return extractChoiceLetters(answer, options == null ? 0 : options.size());
        }
        return textOrEmpty(answer, "");
    }

    private String extractAnswerFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        Matcher matcher = Pattern.compile("(?:答案|正确答案|standard_answer|answer)\\s*[:：为是]?\\s*([A-Ha-h,，、\\s]+|正确|错误|对|错|true|false)", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractChoiceLetters(String answer, int optionCount) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String normalized = answer.trim().toUpperCase(Locale.ROOT)
                .replaceAll("选项", "")
                .replaceAll("[^A-H]", "");
        StringBuilder builder = new StringBuilder();
        for (char c : normalized.toCharArray()) {
            int index = c - 'A';
            if (index >= 0 && (optionCount <= 0 || index < optionCount) && builder.indexOf(String.valueOf(c)) < 0) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String normalizeJudgeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String value = answer.trim().toLowerCase(Locale.ROOT);
        if (Set.of("true", "t", "正确", "对", "是", "yes", "y").contains(value)) {
            return "true";
        }
        if (Set.of("false", "f", "错误", "错", "否", "no", "n").contains(value)) {
            return "false";
        }
        return "";
    }

    private String toOptionsJson(String type, List<String> options) {
        if (!CHOICE_TYPES.contains(type) || options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return null;
        }
    }

    private String inferKnowledgePoint(String content, String outlineText) {
        for (String point : extractKeyPoints(outlineText, 12)) {
            if (StringUtils.hasText(content) && content.contains(point)) {
                return point;
            }
        }
        List<String> points = extractKeyPoints(outlineText, 1);
        return points.isEmpty() ? "课程核心知识" : points.get(0);
    }

    private String firstText(Map<String, Object> map, String... keys) {
        Object value = firstValue(map, keys);
        return value == null ? "" : String.valueOf(value);
    }

    private Object firstValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && (!(value instanceof String text) || StringUtils.hasText(text))) {
                return value;
            }
        }
        return null;
    }

    private Object firstMapValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && (!(value instanceof String text) || StringUtils.hasText(text))) {
                return value;
            }
        }
        return null;
    }

    private String stripOptionPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceFirst("(?i)^[A-H][\\.、．:]\\s*", "").trim();
    }

    private String extractJsonArray(String content) {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end < start) {
            throw new BusinessException(500, "AI返回的出题计划不是JSON数组");
        }
        return content.substring(start, end + 1);
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) return "";
        String value = type.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "SINGLE", "单选", "单选题" -> "SINGLE";
            case "MULTIPLE", "多选", "多选题" -> "MULTIPLE";
            case "JUDGE", "判断", "判断题" -> "JUDGE";
            case "SHORT", "简答", "简答题" -> "SHORT";
            case "CODE", "代码题", "编程题" -> "CODE";
            default -> "";
        };
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) return "MEDIUM";
        String value = difficulty.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "EASY", "简单" -> "EASY";
            case "HARD", "困难", "较难" -> "HARD";
            default -> "MEDIUM";
        };
    }

    private String textOrEmpty(String value, String fallback) {
        if (StringUtils.hasText(value)) return value.trim();
        return StringUtils.hasText(fallback) ? fallback.trim() : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String stripExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(0, index) : filename;
    }
}
