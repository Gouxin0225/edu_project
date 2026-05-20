package com.example.edubackend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AddQuestionsDTO;
import com.example.edubackend.dto.CreateExamDTO;
import com.example.edubackend.dto.ExamQuestionVO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.dto.QuestionExcelExportRow;
import com.example.edubackend.dto.UpdateQuestionScoreDTO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.ITaskQuestionRelService;
import com.example.edubackend.service.QuestionExcelExportService;
import com.example.edubackend.service.QuestionWordExportService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class AssessmentTaskController {

    private final IAssessmentTaskService assessmentTaskService;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final QuestionBankMapper questionBankMapper;
    private final ITaskQuestionRelService taskQuestionRelService;
    private final ObjectMapper objectMapper;
    private final ExamSettingHelper examSettingHelper;
    private final QuestionExcelExportService questionExcelExportService;
    private final QuestionWordExportService questionWordExportService;

    @PostMapping
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ExamVO> createExam(@Valid @RequestBody CreateExamDTO dto) {
        Long userId = UserContext.getUserId();
        ExamVO exam = assessmentTaskService.createExam(dto, userId);
        return Result.success(exam);
    }

    @GetMapping
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<IPage<ExamVO>> listExams(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getUser().getRole();
        
        Page<AssessmentTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AssessmentTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentTask::getType, "EXAM");
        
        if (!"ADMIN".equals(role)) {
            wrapper.eq(AssessmentTask::getCreatorId, userId);
        }
        wrapper.orderByDesc(AssessmentTask::getCreateTime);
        
        IPage<AssessmentTask> pageResult = assessmentTaskMapper.selectPage(pageParam, wrapper);
        
        Page<ExamVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        List<ExamVO> voList = new ArrayList<>();
        for (AssessmentTask exam : pageResult.getRecords()) {
            voList.add(toExamVO(exam));
        }
        voPage.setRecords(voList);
        
        return Result.success(voPage);
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ExamVO> getExam(@PathVariable Long id) {
        AssessmentTask exam = assessmentTaskMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanManageExam(exam);
        return Result.success(toExamVO(exam));
    }

    @GetMapping("/{id}/questions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<ExamQuestionVO>> getExamQuestions(@PathVariable Long id) {
        assertCanManageExam(id);
        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, id)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );
        
        List<ExamQuestionVO> result = new ArrayList<>();
        for (TaskQuestionRel rel : rels) {
            QuestionBank q = questionBankMapper.selectById(rel.getQuestionId());
            if (q != null) {
                ExamQuestionVO vo = new ExamQuestionVO();
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
        return Result.success(result);
    }

    @GetMapping("/{id}/export-paper")
    @RequireRole({"ADMIN", "TEACHER"})
    public void exportExamPaper(@PathVariable Long id, HttpServletResponse response) throws IOException {
        AssessmentTask exam = assessmentTaskMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanManageExam(exam);
        List<ExamQuestionVO> questions = getExamQuestions(id).getData();
        List<QuestionExcelExportRow> rows = questionExcelExportService.fromExamQuestions(questions);
        writeQuestionExcel(response, safeFileName(exam.getTitle()), rows);
    }

    @GetMapping("/{id}/export-word")
    @RequireRole({"ADMIN", "TEACHER"})
    public void exportExamWord(@PathVariable Long id, HttpServletResponse response) throws IOException {
        AssessmentTask exam = assessmentTaskMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanManageExam(exam);
        List<ExamQuestionVO> questions = getExamQuestions(id).getData();
        byte[] bytes = questionWordExportService.exportExamToWord(questions);

        String fileName = URLEncoder.encode(safeFileName(exam.getTitle()) + ".docx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.getOutputStream().write(bytes);
    }

    @PostMapping("/{id}/questions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> addQuestions(@PathVariable Long id, @Valid @RequestBody AddQuestionsDTO dto) {
        assertCanManageExam(id);
        assessmentTaskService.addQuestions(id, dto.getQuestionIds());
        return Result.success("题目已添加");
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> removeQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        assertCanManageExam(id);
        assessmentTaskService.removeQuestion(id, questionId);
        return Result.success("题目已移除");
    }

    @PostMapping("/{id}/auto-score")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> autoScore(@PathVariable Long id) {
        assertCanManageExam(id);
        assessmentTaskService.autoScore(id);
        return Result.success("100分智能分配完成");
    }

    @PutMapping("/{id}/question/{questionId}/score")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> updateQuestionScore(
            @PathVariable Long id,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionScoreDTO dto) {
        assertCanManageExam(id);
        assessmentTaskService.updateQuestionScore(id, questionId, dto.getScore());
        return Result.success("分值已更新");
    }

    @PostMapping("/{id}/publish")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> publishExam(@PathVariable Long id) {
        assertCanManageExam(id);
        assessmentTaskService.publishExam(id);
        return Result.success("考试已发布");
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> deleteExam(@PathVariable Long id) {
        assertCanManageExam(id);
        assessmentTaskMapper.deleteById(id);
        return Result.success("考试已删除");
    }

    private void assertCanManageExam(Long examId) {
        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanManageExam(exam);
    }

    private void assertCanManageExam(AssessmentTask exam) {
        if (!"EXAM".equals(exam.getType())) {
            throw new BusinessException(400, "任务不是考试");
        }
        String role = UserContext.getUser().getRole();
        if (!"ADMIN".equals(role) && !exam.getCreatorId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只能管理自己创建的考试");
        }
    }

    private ExamVO toExamVO(AssessmentTask exam) {
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
        vo.setTargetClassIds(parseTargetClassIds(exam.getTargetClassIds()));
        return vo;
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
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
            return "考试试卷";
        }
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
