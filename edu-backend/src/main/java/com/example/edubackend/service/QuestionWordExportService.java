package com.example.edubackend.service;

import com.example.edubackend.dto.ExamQuestionVO;
import com.example.edubackend.dto.TemplateQuestionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionWordExportService {

    private final ObjectMapper objectMapper;

    public byte[] exportExamToWord(List<ExamQuestionVO> questions) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int i = 0; i < questions.size(); i++) {
                ExamQuestionVO q = questions.get(i);
                addQuestion(document, i + 1, q.getType(), q.getContent(), q.getOptionsJson(), q.getStandardAnswer(), q.getAnalysis());
            }

            document.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTemplateToWord(List<TemplateQuestionVO> questions) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int i = 0; i < questions.size(); i++) {
                TemplateQuestionVO q = questions.get(i);
                addQuestion(document, i + 1, q.getType(), q.getContent(), q.getOptionsJson(), q.getStandardAnswer(), q.getAnalysis());
            }

            document.write(out);
            return out.toByteArray();
        }
    }

    private void addQuestion(XWPFDocument document, int index, String type, String content, String optionsJson, String standardAnswer, String analysis) {
        XWPFParagraph p = document.createParagraph();
        XWPFRun run = p.createRun();
        
        String prefix = "";
        if ("MULTIPLE".equals(type)) {
            prefix = "[不定项选择题]";
        }

        // 1. 题目正文
        run.setText(index + "." + prefix + content);
        run.addBreak();

        // 2. 选项 (仅选择题)
        if ("SINGLE".equals(type) || "MULTIPLE".equals(type)) {
            List<String> options = parseOptions(optionsJson);
            for (int j = 0; j < options.size(); j++) {
                XWPFRun optRun = document.createParagraph().createRun();
                char letter = (char) ('A' + j);
                optRun.setText(letter + "、" + options.get(j));
            }
        }

        // 3. 答案
        XWPFParagraph ansP = document.createParagraph();
        XWPFRun ansRun = ansP.createRun();
        ansRun.setText("答案：" + formatAnswer(type, standardAnswer));

        // 4. 解析
        if (StringUtils.hasText(analysis)) {
            XWPFParagraph anaP = document.createParagraph();
            XWPFRun anaRun = anaP.createRun();
            anaRun.setText("解析：" + analysis);
        }

        // 5. 空行分隔
        document.createParagraph();
    }

    private String formatAnswer(String type, String answer) {
        if ("JUDGE".equals(type)) {
            String val = answer.trim().toLowerCase(Locale.ROOT);
            if (List.of("true", "1", "正确", "对", "是").contains(val)) return "正确";
            if (List.of("false", "0", "错误", "错", "否").contains(val)) return "错误";
            return answer;
        }
        
        if ("SINGLE".equals(type) || "MULTIPLE".equals(type)) {
            return answer.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s,，、;；]+", "");
        }
        
        return answer != null ? answer.trim() : "";
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
                if (!options.isEmpty()) return options;
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    options.add(fields.next().getValue().asText(""));
                }
                return options;
            }
        } catch (Exception ignored) {}
        return List.of();
    }
}
