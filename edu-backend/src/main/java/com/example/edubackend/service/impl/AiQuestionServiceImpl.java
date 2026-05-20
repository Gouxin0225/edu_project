package com.example.edubackend.service.impl;

import com.example.edubackend.dto.AiGenerateQuestionDTO;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.service.IAiQuestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionServiceImpl implements IAiQuestionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.endpoint:https://api.deepseek.com/v1/chat/completions}")
    private String apiEndpoint;

    @Value("${ai.api.model:deepseek-chat}")
    private String model;

    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_MS = 120000;
    private static final Set<String> OBJECTIVE_TYPES = Set.of("SINGLE", "MULTIPLE", "JUDGE");
    private static final Set<String> CHOICE_TYPES = Set.of("SINGLE", "MULTIPLE");
    private static final Set<String> VALID_TYPES = Set.of("SINGLE", "MULTIPLE", "JUDGE", "SHORT", "CODE");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    @Override
    public List<QuestionBank> generateQuestions(AiGenerateQuestionDTO dto, Long creatorId) {
        assertAiConfigured();

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(dto);

        List<Map<String, Object>> questionDataList = generateAndParseQuestions(systemPrompt, userPrompt, dto);
        return convertToQuestionBanks(questionDataList, creatorId, dto.getCourseCategory(), dto.getKnowledgePoint());
    }

    private List<Map<String, Object>> generateAndParseQuestions(String systemPrompt, String userPrompt, AiGenerateQuestionDTO dto) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("AI生成题目第{}次尝试", attempt);
                String rawResponse = callAiApi(systemPrompt, userPrompt);
                return parseAndValidateJson(rawResponse, dto);
            } catch (Exception e) {
                lastException = e;
                log.warn("AI生成题目第{}次尝试失败: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt);
                }
            }
        }
        throw new BusinessException(500, "AI生成失败: " + (lastException == null ? "未知错误" : lastException.getMessage()));
    }

    private String buildSystemPrompt() {
        return """
            你是一个专业的教育题目生成专家。
            你的任务是根据用户提供的知识点、题型和难度生成题目。
            
            严格遵守以下规则：
            1. 只输出JSON数组，不要输出任何其他内容
            2. JSON数组中的每个对象必须包含以下字段：
               - type: 题型，只能是 SINGLE(单选), MULTIPLE(多选), JUDGE(判断), SHORT(简答), CODE(编程) 之一
               - difficulty: 难度，只能是 EASY, MEDIUM, HARD 之一
               - content: 题干内容，清晰明确
               - options_json: 单选和多选题必须给出JSON数组，例如 ["选项A", "选项B", "选项C", "选项D"]；判断、简答、编程题为null
               - standard_answer: 标准答案
               - analysis: 题目解析
            
            3. 确保题目质量：
               - 题干表述清晰，无歧义
               - 选项设置合理，有区分度
               - 答案准确无误
               - 解析详细清楚
               - SINGLE 的 standard_answer 必须是一个大写字母，如 "A"
               - MULTIPLE 的 standard_answer 必须是多个大写字母连续写法，如 "AC"
               - JUDGE 的 standard_answer 必须是 "true" 或 "false"
               - SHORT 和 CODE 的 standard_answer 必须给出完整参考答案
            
            4. 只输出JSON，不要有任何前缀或后缀文字
            """;
    }

    private String buildUserPrompt(AiGenerateQuestionDTO dto) {
        String types = String.join(", ", dto.getTypes());
        String topic = StringUtils.hasText(dto.getKnowledgePoint()) ? dto.getKnowledgePoint().trim() : "该课程方向的核心知识";
        String contextBlock = StringUtils.hasText(dto.getContext())
                ? "\n参考资料/出题范围：\n" + dto.getContext().trim() + "\n"
                : "";
        return String.format("""
            请生成%d道关于"%s"的题目。

            要求：
            - 课程方向：%s
            - 题型：%s，生成结果中每道题都必须填写 type 字段
            - 难度：%s
            %s
            - 每种题型尽量均匀分布
            - 如果未给出具体知识点，请围绕课程方向自动选择合适知识点，但输出字段中不需要额外增加 knowledgePoint

            请直接输出JSON数组，不要有任何其他内容。
            """, dto.getCount(), topic, dto.getCourseCategory(), types, dto.getDifficulty(), contextBlock);
    }

    private String callAiApiWithRetry(String systemPrompt, String userPrompt) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("AI生成题目第{}次尝试", attempt);
                return callAiApi(systemPrompt, userPrompt);
            } catch (Exception e) {
                lastException = e;
                log.warn("AI生成题目第{}次尝试失败: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt);
                }
            }
        }
        
        throw new BusinessException(500, "AI生成失败: " + lastException.getMessage());
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(1000L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String callAiApi(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 8192);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                apiEndpoint,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new BusinessException(500, "AI API调用失败: " + response.getStatusCode());
        }

        return parseAiResponse(response.getBody());
    }

    private String parseAiResponse(String responseBody) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            if (responseMap.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                String errorMessage = (String) error.getOrDefault("message", "Unknown error");
                throw new BusinessException(500, "AI API错误: " + errorMessage);
            }
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(500, "AI API返回为空");
            }
            
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "解析AI响应失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> parseAndValidateJson(String rawResponse, AiGenerateQuestionDTO dto) {
        String jsonContent = extractJsonArray(rawResponse);
        
        try {
            List<Map<String, Object>> result = objectMapper.readValue(
                    jsonContent, 
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            normalizeQuestionList(result, dto);
            
            validateQuestionList(result);
            
            return result;
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败, 原始内容: {}", rawResponse);
            throw new BusinessException(400, "AI返回内容格式错误，非合法JSON数组");
        }
    }

    private String extractJsonArray(String content) {
        content = content.trim();
        
        int jsonStart = content.indexOf('[');
        int jsonEnd = content.lastIndexOf(']');
        
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart > jsonEnd) {
            String cleaned = content.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();
            jsonStart = cleaned.indexOf('[');
            jsonEnd = cleaned.lastIndexOf(']');
            if (jsonStart == -1 || jsonEnd == -1) {
                throw new BusinessException(400, "AI返回内容中未找到JSON数组");
            }
            return cleaned.substring(jsonStart, jsonEnd + 1);
        }
        
        return content.substring(jsonStart, jsonEnd + 1);
    }

    private void validateQuestionList(List<Map<String, Object>> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new BusinessException(400, "AI返回的题目列表为空");
        }

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            
            String type = (String) q.get("type");
            if (type == null || !VALID_TYPES.contains(type.toUpperCase())) {
                throw new BusinessException(400, String.format("第%d题: 题型'%s'不合法", i + 1, type));
            }
            
            String difficulty = (String) q.get("difficulty");
            if (difficulty == null || !VALID_DIFFICULTIES.contains(difficulty.toUpperCase())) {
                throw new BusinessException(400, String.format("第%d题: 难度'%s'不合法", i + 1, difficulty));
            }
            
            String content = (String) q.get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException(400, String.format("第%d题: 题干内容为空", i + 1));
            }
            
            String standardAnswer = (String) q.get("standard_answer");
            if (standardAnswer == null || standardAnswer.trim().isEmpty()) {
                throw new BusinessException(400, String.format("第%d题: 标准答案为空", i + 1));
            }

            if (CHOICE_TYPES.contains(type)) {
                Object optionsObj = q.get("options_json");
                if (!(optionsObj instanceof List<?> options) || options.size() < 2) {
                    throw new BusinessException(400, String.format("第%d题: 选择题选项为空或不足", i + 1));
                }
                if (!isValidChoiceAnswer(type, standardAnswer, options.size())) {
                    throw new BusinessException(400, String.format("第%d题: 选择题答案'%s'不合法", i + 1, standardAnswer));
                }
            }
        }
    }

    private List<QuestionBank> convertToQuestionBanks(
            List<Map<String, Object>> questionDataList,
            Long creatorId,
            String courseCategory,
            String fallbackKnowledgePoint) {
        List<QuestionBank> questions = new ArrayList<>();
        
        for (Map<String, Object> data : questionDataList) {
            QuestionBank question = new QuestionBank();
            String type = (String) data.get("type");
            question.setType(type);
            question.setDifficulty((String) data.get("difficulty"));
            question.setContent((String) data.get("content"));
            question.setKnowledgePoint(textOrEmpty(firstText(data, "knowledge_point", "knowledgePoint", "知识点"), fallbackKnowledgePoint));
            
            Object optionsObj = data.get("options_json");
            if (CHOICE_TYPES.contains(type) && optionsObj != null) {
                try {
                    question.setOptionsJson(objectMapper.writeValueAsString(optionsObj));
                } catch (Exception e) {
                    question.setOptionsJson(null);
                }
            }
            
            question.setStandardAnswer((String) data.get("standard_answer"));
            question.setAnalysis((String) data.get("analysis"));
            question.setCreatorId(creatorId);
            question.setIsAiGenerated((byte) 1);
            question.setIsPublic((byte) 1);  // AI生成的题目也默认公开
            question.setCourseCategory(courseCategory);
            
            questions.add(question);
        }
        
        return questions;
    }

    private void normalizeQuestionList(List<Map<String, Object>> questions, AiGenerateQuestionDTO dto) {
        if (questions == null) {
            return;
        }
        List<String> requestedTypes = dto.getTypes() == null ? List.of() : dto.getTypes();
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            String fallbackType = requestedTypes.isEmpty() ? "SHORT" : requestedTypes.get(i % requestedTypes.size());
            String type = normalizeType(firstText(q, "type", "question_type", "题型"), fallbackType);
            String difficulty = normalizeDifficulty(firstText(q, "difficulty", "level", "难度"), dto.getDifficulty());
            String content = firstText(q, "content", "question", "title", "题干");
            String analysis = firstText(q, "analysis", "explanation", "解析");
            List<String> options = normalizeOptions(type, firstValue(q, "options_json", "options", "choices", "选项"));
            String answer = firstText(q, "standard_answer", "standardAnswer", "answer", "correct_answer", "correctAnswer", "参考答案", "标准答案");
            answer = normalizeAnswer(type, answer, options, analysis);

            q.put("type", type);
            q.put("difficulty", difficulty);
            q.put("content", content);
            q.put("knowledge_point", textOrEmpty(firstText(q, "knowledge_point", "knowledgePoint", "知识点")));
            q.put("options_json", CHOICE_TYPES.contains(type) ? options : null);
            q.put("standard_answer", answer);
            q.put("analysis", analysis);
        }
    }

    private String normalizeType(String rawType, String fallbackType) {
        String value = StringUtils.hasText(rawType) ? rawType.trim().toUpperCase(Locale.ROOT) : "";
        return switch (value) {
            case "SINGLE", "SINGLE_CHOICE", "单选", "单选题" -> "SINGLE";
            case "MULTIPLE", "MULTIPLE_CHOICE", "多选", "多选题" -> "MULTIPLE";
            case "JUDGE", "TRUE_FALSE", "判断", "判断题" -> "JUDGE";
            case "SHORT", "SHORT_ANSWER", "简答", "简答题" -> "SHORT";
            case "CODE", "PROGRAMMING", "编程", "编程题", "代码题" -> "CODE";
            default -> VALID_TYPES.contains(fallbackType) ? fallbackType : "SHORT";
        };
    }

    private String normalizeDifficulty(String rawDifficulty, String fallbackDifficulty) {
        String value = StringUtils.hasText(rawDifficulty) ? rawDifficulty.trim().toUpperCase(Locale.ROOT) : "";
        return switch (value) {
            case "EASY", "简单" -> "EASY";
            case "MEDIUM", "中等", "普通" -> "MEDIUM";
            case "HARD", "困难", "较难" -> "HARD";
            default -> VALID_DIFFICULTIES.contains(fallbackDifficulty) ? fallbackDifficulty : "MEDIUM";
        };
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
                    .sorted(Comparator.comparing(e -> String.valueOf(e.getKey())))
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
            List<String> parsed = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            return parsed.stream().map(this::stripOptionPrefix).filter(StringUtils::hasText).toList();
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
            if (StringUtils.hasText(letters)) {
                return letters.substring(0, 1);
            }
            return "";
        }
        if ("MULTIPLE".equals(type)) {
            return extractChoiceLetters(answer, options == null ? 0 : options.size());
        }
        return textOrEmpty(answer);
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
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
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

    private boolean isValidChoiceAnswer(String type, String answer, int optionCount) {
        if (!StringUtils.hasText(answer)) {
            return false;
        }
        String value = answer.trim().toUpperCase(Locale.ROOT);
        if ("SINGLE".equals(type) && value.length() != 1) {
            return false;
        }
        if (!value.matches("[A-H]+")) {
            return false;
        }
        for (char c : value.toCharArray()) {
            int index = c - 'A';
            if (index < 0 || index >= optionCount) {
                return false;
            }
        }
        return true;
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

    private String textOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String textOrEmpty(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : "";
    }

    @Override
    public com.example.edubackend.dto.AIGradeSuggestVO getGradeSuggestion(com.example.edubackend.dto.AIGradeSuggestDTO dto) {
        BigDecimal maxScore = normalizeMaxScore(dto.getMaxScore());
        if (!isAiConfigured()) {
            return fallbackGradeSuggestion(maxScore, "AI服务未配置API Key，请教师手动评分。");
        }

        String systemPrompt = """
            你是一个严谨的考试主观题批改助手，负责给教师提供评分建议。
            
            评分规则：
            1. 必须基于题目、标准答案、学生答案和满分进行评分。
            2. 分数必须在 0 到 maxScore 之间，可以保留 1 位小数。
            3. 简答题重点看关键点覆盖、概念准确性、逻辑完整性。
            4. 编程题重点看核心思路、边界处理、语法/伪代码可执行性、输出是否符合题意。
            5. 学生未作答、明显跑题、只复述题干时应给低分或 0 分。
            6. 不要因为表述与标准答案不同就简单扣分；等价表达、合理补充可以认可。
            
            输出格式（严格JSON）：
            {
                "suggestedScore": 数字,
                "maxScore": 数字,
                "reasoning": "一句完整评分理由，说明为什么给这个分",
                "keyPoints": ["本题主要采分点1", "本题主要采分点2"],
                "matchedPoints": ["学生答案已命中的点"],
                "missingPoints": ["学生答案缺失或错误的点"],
                "confidence": "HIGH/MEDIUM/LOW"
            }
            
            只输出JSON，不要有任何前缀或后缀文字。
            """;

        String userPrompt = String.format("""
            题型：%s
            满分：%s

            题目内容：%s
            
            标准答案：%s
            
            学生答案：%s

            额外评分标准：%s
            
            请给出建议得分、原因、命中点和缺失点。
            """,
                textOrEmpty(dto.getQuestionType(), "主观题"),
                maxScore.stripTrailingZeros().toPlainString(),
                dto.getQuestionContent(),
                dto.getStandardAnswer(),
                textOrEmpty(dto.getStudentAnswer(), "未作答"),
                textOrEmpty(dto.getScoringRubric(), "无"));

        try {
            String rawResponse = callAiApiWithRetry(systemPrompt, userPrompt);
            return parseGradeSuggestion(rawResponse, maxScore);
        } catch (Exception e) {
            log.error("AI评分建议失败", e);
            return fallbackGradeSuggestion(maxScore, "AI评分服务暂时不可用，请教师手动评分。");
        }
    }

    private com.example.edubackend.dto.AIGradeSuggestVO parseGradeSuggestion(String rawResponse, BigDecimal requestedMaxScore) {
        String jsonContent = extractJsonObject(rawResponse);
        try {
            Map<String, Object> result = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
            com.example.edubackend.dto.AIGradeSuggestVO vo = new com.example.edubackend.dto.AIGradeSuggestVO();

            BigDecimal maxScore = normalizeMaxScore(requestedMaxScore);
            BigDecimal suggestedScore = parseBigDecimal(firstMapValue(result, "suggestedScore", "suggested_score", "score", "得分", "建议得分"));
            vo.setMaxScore(maxScore);
            vo.setSuggestedScore(clampScore(suggestedScore == null ? BigDecimal.ZERO : suggestedScore, maxScore));

            String reasoning = textOrEmpty(firstText(result, "reasoning", "reason", "suggestion", "原因", "评分理由"));
            vo.setReasoning(reasoning);
            vo.setSuggestion(reasoning);
            vo.setKeyPoints(parseStringList(firstMapValue(result, "keyPoints", "key_points", "rubric", "采分点")));
            vo.setMatchedPoints(parseStringList(firstMapValue(result, "matchedPoints", "matched_points", "命中点")));
            vo.setMissingPoints(parseStringList(firstMapValue(result, "missingPoints", "missing_points", "缺失点", "扣分点")));
            vo.setConfidence(normalizeConfidence(firstText(result, "confidence", "置信度")));
            return vo;
        } catch (Exception e) {
            log.error("解析AI评分建议失败, 原始内容: {}", rawResponse);
            return fallbackGradeSuggestion(requestedMaxScore, "解析AI响应失败，请教师手动评分。");
        }
    }

    private com.example.edubackend.dto.AIGradeSuggestVO fallbackGradeSuggestion(BigDecimal maxScore, String reason) {
        com.example.edubackend.dto.AIGradeSuggestVO fallback = new com.example.edubackend.dto.AIGradeSuggestVO();
        fallback.setSuggestedScore(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        fallback.setMaxScore(normalizeMaxScore(maxScore));
        fallback.setReasoning(reason);
        fallback.setSuggestion(reason);
        fallback.setConfidence("LOW");
        return fallback;
    }

    private BigDecimal normalizeMaxScore(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.TEN;
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal clampScore(BigDecimal score, BigDecimal maxScore) {
        BigDecimal normalizedScore = score.setScale(1, RoundingMode.HALF_UP);
        if (normalizedScore.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        if (normalizedScore.compareTo(maxScore) > 0) {
            return maxScore;
        }
        return normalizedScore;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                Matcher matcher = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(text);
                return matcher.find() ? new BigDecimal(matcher.group()) : null;
            }
        }
        return null;
    }

    private String normalizeConfidence(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "MEDIUM";
        return Set.of("HIGH", "MEDIUM", "LOW").contains(normalized) ? normalized : "MEDIUM";
    }

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> item == null ? "" : String.valueOf(item).trim())
                    .filter(StringUtils::hasText)
                    .toList();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Arrays.stream(text.split("[\\n;；]+"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return new ArrayList<>();
    }

    private String extractJsonObject(String content) {
        content = content.trim();
        
        int jsonStart = content.indexOf('{');
        int jsonEnd = content.lastIndexOf('}');
        
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart > jsonEnd) {
            String cleaned = content.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();
            jsonStart = cleaned.indexOf('{');
            jsonEnd = cleaned.lastIndexOf('}');
            if (jsonStart == -1 || jsonEnd == -1) {
                throw new BusinessException(400, "AI返回内容中未找到JSON对象");
            }
            return cleaned.substring(jsonStart, jsonEnd + 1);
        }
        
        return content.substring(jsonStart, jsonEnd + 1);
    }

    private void assertAiConfigured() {
        if (!isAiConfigured()) {
            throw new BusinessException(500, "AI服务未配置API Key，请在后端运行环境配置 AI_API_KEY 后重启 edu-backend");
        }
    }

    private boolean isAiConfigured() {
        return StringUtils.hasText(apiKey);
    }
}
