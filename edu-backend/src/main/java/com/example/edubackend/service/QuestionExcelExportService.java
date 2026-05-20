package com.example.edubackend.service;

import com.example.edubackend.dto.ExamQuestionVO;
import com.example.edubackend.dto.QuestionExcelExportRow;
import com.example.edubackend.dto.TemplateQuestionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionExcelExportService {

    private static final String SINGLE = "SINGLE";
    private static final String MULTIPLE = "MULTIPLE";
    private static final String JUDGE = "JUDGE";

    private final ObjectMapper objectMapper;

    public List<QuestionExcelExportRow> fromTemplateQuestions(List<TemplateQuestionVO> questions) {
        return questions.stream()
                .map(question -> toRow(
                        question.getCourseCategory(),
                        question.getKnowledgePoint(),
                        question.getType(),
                        question.getDifficulty(),
                        question.getContent(),
                        question.getOptionsJson(),
                        question.getStandardAnswer(),
                        question.getAnalysis()))
                .toList();
    }

    public List<QuestionExcelExportRow> fromExamQuestions(List<ExamQuestionVO> questions) {
        return questions.stream()
                .map(question -> toRow(
                        question.getCourseCategory(),
                        question.getKnowledgePoint(),
                        question.getType(),
                        question.getDifficulty(),
                        question.getContent(),
                        question.getOptionsJson(),
                        question.getStandardAnswer(),
                        question.getAnalysis()))
                .toList();
    }

    private QuestionExcelExportRow toRow(
            String courseCategory,
            String knowledgePoint,
            String type,
            String difficulty,
            String content,
            String optionsJson,
            String standardAnswer,
            String analysis) {
        List<String> options = parseOptions(optionsJson);
        QuestionExcelExportRow row = new QuestionExcelExportRow();
        row.setCourseCategory(nullToEmpty(courseCategory));
        row.setKnowledgePoint(nullToEmpty(knowledgePoint));
        row.setType(nullToEmpty(type));
        row.setDifficulty(nullToEmpty(difficulty));
        row.setContent(nullToEmpty(content));
        row.setOptionA(optionAt(options, 0));
        row.setOptionB(optionAt(options, 1));
        row.setOptionC(optionAt(options, 2));
        row.setOptionD(optionAt(options, 3));
        row.setStandardAnswer(normalizeAnswer(type, standardAnswer, options));
        row.setAnalysis(nullToEmpty(analysis));
        return row;
    }

    private List<String> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(optionsJson);
            List<String> options = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    options.add(node.asText(""));
                }
                return options;
            }
            if (root.isObject()) {
                for (String key : List.of("A", "B", "C", "D", "E", "F", "G", "H")) {
                    JsonNode node = root.get(key);
                    if (node != null) {
                        options.add(node.asText(""));
                    }
                }
                if (!options.isEmpty()) {
                    return options;
                }
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    options.add(fields.next().getValue().asText(""));
                }
                return options;
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private String normalizeAnswer(String type, String answer, List<String> options) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        if (JUDGE.equals(type)) {
            return normalizeJudgeAnswer(answer);
        }
        if (SINGLE.equals(type) || MULTIPLE.equals(type)) {
            String letters = normalizeChoiceLetters(answer, options);
            return StringUtils.hasText(letters) ? letters : answer.trim();
        }
        return answer.trim();
    }

    private String normalizeJudgeAnswer(String answer) {
        String value = answer.trim().toLowerCase(Locale.ROOT);
        if (List.of("true", "t", "1", "yes", "y", "正确", "对", "是").contains(value)) {
            return "true";
        }
        if (List.of("false", "f", "0", "no", "n", "错误", "错", "否").contains(value)) {
            return "false";
        }
        return answer.trim();
    }

    private String normalizeChoiceLetters(String answer, List<String> options) {
        String compact = answer.trim()
                .replaceAll("[\\s,，、;；]+", "")
                .toUpperCase(Locale.ROOT);
        if (compact.matches("^[A-H]+$")) {
            return dedupeLetters(compact);
        }

        StringBuilder result = new StringBuilder();
        String normalizedAnswer = answer.trim();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (!StringUtils.hasText(option)) {
                continue;
            }
            String trimmedOption = option.trim();
            if (normalizedAnswer.equals(trimmedOption) || normalizedAnswer.contains(trimmedOption)) {
                char letter = (char) ('A' + i);
                if (result.indexOf(String.valueOf(letter)) < 0) {
                    result.append(letter);
                }
            }
        }
        return result.toString();
    }

    private String dedupeLetters(String letters) {
        StringBuilder result = new StringBuilder();
        for (char c : letters.toCharArray()) {
            if (result.indexOf(String.valueOf(c)) < 0) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String optionAt(List<String> options, int index) {
        return options.size() > index ? nullToEmpty(options.get(index)) : "";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
