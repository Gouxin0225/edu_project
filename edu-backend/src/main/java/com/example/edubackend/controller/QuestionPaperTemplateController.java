package com.example.edubackend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateExamFromTemplateDTO;
import com.example.edubackend.dto.CreateTemplateDTO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.dto.QuestionExcelExportRow;
import com.example.edubackend.dto.TemplateQuestionVO;
import com.example.edubackend.dto.TemplateVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IQuestionPaperTemplateService;
import com.example.edubackend.service.QuestionExcelExportService;
import com.example.edubackend.service.QuestionWordExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
public class QuestionPaperTemplateController {

    private final IQuestionPaperTemplateService templateService;
    private final QuestionExcelExportService questionExcelExportService;
    private final QuestionWordExportService questionWordExportService;

    @PostMapping
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<TemplateVO> createTemplate(@Valid @RequestBody CreateTemplateDTO dto) {
        Long userId = UserContext.getUserId();
        String userName = UserContext.getUser().getRealName();
        TemplateVO template = templateService.createTemplate(dto, userId, userName);
        return Result.success(template);
    }

    @GetMapping
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<IPage<TemplateVO>> listTemplates(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        IPage<TemplateVO> pageResult = templateService.listTemplates(page, size, userId);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<TemplateVO> getTemplate(@PathVariable Long id) {
        TemplateVO template = templateService.getTemplateById(id);
        return Result.success(template);
    }

    @GetMapping("/{id}/questions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<TemplateQuestionVO>> getTemplateQuestions(@PathVariable Long id) {
        List<TemplateQuestionVO> questions = templateService.getTemplateQuestions(id);
        return Result.success(questions);
    }

    @GetMapping("/{id}/export")
    @RequireRole({"ADMIN", "TEACHER"})
    public void exportTemplate(@PathVariable Long id, HttpServletResponse response) throws IOException {
        TemplateVO template = templateService.getTemplateById(id);
        List<TemplateQuestionVO> questions = templateService.getTemplateQuestions(id);
        List<QuestionExcelExportRow> rows = questionExcelExportService.fromTemplateQuestions(questions);
        writeQuestionExcel(response, safeFileName(template.getName()), rows);
    }

    @GetMapping("/{id}/export-word")
    @RequireRole({"ADMIN", "TEACHER"})
    public void exportTemplateWord(@PathVariable Long id, HttpServletResponse response) throws IOException {
        TemplateVO template = templateService.getTemplateById(id);
        List<TemplateQuestionVO> questions = templateService.getTemplateQuestions(id);
        byte[] bytes = questionWordExportService.exportTemplateToWord(questions);

        String fileName = URLEncoder.encode(safeFileName(template.getName()) + ".docx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.getOutputStream().write(bytes);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.success("模板已删除");
    }

    @PostMapping("/create-exam")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ExamVO> createExamFromTemplate(@Valid @RequestBody CreateExamFromTemplateDTO dto) {
        Long userId = UserContext.getUserId();
        ExamVO exam = templateService.createExamFromTemplate(dto, userId);
        return Result.success(exam);
    }

    private void writeQuestionExcel(
            HttpServletResponse response,
            String fileName,
            List<QuestionExcelExportRow> rows) throws IOException {
        String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        EasyExcel.write(response.getOutputStream(), QuestionExcelExportRow.class)
                .sheet("题目导入模板")
                .doWrite(rows);
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "试卷模板";
        }
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
