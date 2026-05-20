package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AIGradeSuggestDTO;
import com.example.edubackend.dto.AIGradeSuggestVO;
import com.example.edubackend.dto.ExamParticipationVO;
import com.example.edubackend.dto.GradeItemDTO;
import com.example.edubackend.dto.SubmissionDetailVO;
import com.example.edubackend.dto.SubmissionListVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAiQuestionService;
import com.example.edubackend.service.IStudentSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final IStudentSubmissionService submissionService;
    private final IAiQuestionService aiQuestionService;
    private final AssessmentTaskMapper assessmentTaskMapper;

    @GetMapping("/api/exam/{id}/submissions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<SubmissionListVO>> getSubmissionList(@PathVariable Long id) {
        assertCanManageExam(id);
        List<SubmissionListVO> list = submissionService.getSubmissionList(id);
        return Result.success(list);
    }

    @GetMapping("/api/exam/{id}/submission/{submissionId}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<SubmissionDetailVO> getSubmissionDetail(
            @PathVariable Long id,
            @PathVariable Long submissionId) {
        assertCanManageExam(id);
        SubmissionDetailVO detail = submissionService.getSubmissionDetail(id, submissionId);
        return Result.success(detail);
    }

    @PutMapping("/api/exam/{id}/submission/{submissionId}/grade")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> gradeSubmission(
            @PathVariable Long id,
            @PathVariable Long submissionId,
            @Valid @RequestBody List<GradeItemDTO> gradeItems) {
        assertCanManageExam(id);
        submissionService.gradeSubmission(id, submissionId, gradeItems);
        return Result.success("批改完成");
    }

    @PostMapping("/api/exam/{id}/publish-score")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> publishScore(@PathVariable Long id) {
        assertCanManageExam(id);
        submissionService.publishScore(id);
        return Result.success("成绩已发布");
    }

    @PostMapping("/api/question/ai-grade-suggest")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<AIGradeSuggestVO> getGradeSuggestion(@Valid @RequestBody AIGradeSuggestDTO dto) {
        AIGradeSuggestVO result = aiQuestionService.getGradeSuggestion(dto);
        return Result.success(result);
    }

    @GetMapping("/api/exam/{id}/participation")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ExamParticipationVO> getExamParticipation(@PathVariable Long id) {
        assertCanManageExam(id);
        ExamParticipationVO vo = submissionService.getExamParticipation(id);
        return Result.success(vo);
    }

    private void assertCanManageExam(Long examId) {
        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        if (!"EXAM".equals(exam.getType())) {
            throw new BusinessException(400, "任务不是考试");
        }
        String role = UserContext.getUser().getRole();
        if (!"ADMIN".equals(role) && !exam.getCreatorId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只能管理自己创建的考试");
        }
    }
}
