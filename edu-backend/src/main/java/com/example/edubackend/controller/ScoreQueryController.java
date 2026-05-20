package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ExamStatisticsVO;
import com.example.edubackend.dto.StudentExamRecordVO;
import com.example.edubackend.dto.StudentExamListVO;
import com.example.edubackend.dto.StudentExamResultVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.IStudentSubmissionService;
import com.alibaba.excel.EasyExcel;
import com.example.edubackend.dto.ScoreExportDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScoreQueryController {

    private final IAssessmentTaskService assessmentTaskService;
    private final IStudentSubmissionService submissionService;
    private final AssessmentTaskMapper assessmentTaskMapper;

    @GetMapping("/exam/{id}/statistics")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ExamStatisticsVO> getStatistics(@PathVariable Long id) {
        assertCanManageExam(id);
        ExamStatisticsVO statistics = assessmentTaskService.getStatistics(id);
        return Result.success(statistics);
    }

    @GetMapping("/student/exam-records")
    @RequireRole({"STUDENT"})
    public Result<List<StudentExamRecordVO>> getStudentExamRecords() {
        Long studentId = UserContext.getUserId();
        List<StudentExamRecordVO> records = submissionService.getStudentExamRecords(studentId);
        return Result.success(records);
    }

    @GetMapping("/student/exam-list")
    @RequireRole({"STUDENT"})
    public Result<List<StudentExamListVO>> getStudentExamList() {
        Long studentId = UserContext.getUserId();
        List<StudentExamListVO> examList = submissionService.getStudentExamList(studentId);
        return Result.success(examList);
    }

    @GetMapping("/exam/{id}/my-result")
    @RequireRole({"STUDENT"})
    public Result<StudentExamResultVO> getMyResult(@PathVariable Long id) {
        Long studentId = UserContext.getUserId();
        StudentExamResultVO result = submissionService.getStudentExamResult(id, studentId);
        return Result.success(result);
    }

    @GetMapping("/exam/{id}/export")
    @RequireRole({"ADMIN", "TEACHER"})
    public void exportScores(@PathVariable Long id, HttpServletResponse response) throws IOException {
        assertCanManageExam(id);
        List<ScoreExportDTO> data = submissionService.getExportData(id);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("成绩单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ScoreExportDTO.class)
                .sheet("成绩单")
                .doWrite(data);
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
