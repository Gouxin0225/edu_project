package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ExamProgressVO;
import com.example.edubackend.dto.ExamStartVO;
import com.example.edubackend.dto.SaveAnswerDTO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IExamTakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamTakingController {

    private final IExamTakingService examTakingService;

    @PostMapping("/{id}/start")
    @RequireRole({"STUDENT"})
    public Result<ExamStartVO> startExam(@PathVariable Long id) {
        Long studentId = UserContext.getUserId();
        ExamStartVO result = examTakingService.startExam(id, studentId);
        return Result.success(result);
    }

    @GetMapping("/{id}/resume")
    @RequireRole({"STUDENT"})
    public Result<ExamProgressVO> resumeExam(@PathVariable Long id) {
        Long studentId = UserContext.getUserId();
        ExamProgressVO result = examTakingService.resumeExam(id, studentId);
        return Result.success(result);
    }

    @PostMapping("/{id}/save")
    @RequireRole({"STUDENT"})
    public Result<Void> saveAnswers(@PathVariable Long id, @Valid @RequestBody SaveAnswerDTO dto) {
        Long studentId = UserContext.getUserId();
        examTakingService.saveAnswers(id, studentId, dto);
        return Result.success("答案已保存");
    }

    @PostMapping("/{id}/submit")
    @RequireRole({"STUDENT"})
    public Result<Void> submitExam(@PathVariable Long id) {
        Long studentId = UserContext.getUserId();
        examTakingService.submitExam(id, studentId);
        return Result.success("交卷成功");
    }

    @PostMapping("/{id}/screen-switch")
    @RequireRole({"STUDENT"})
    public Result<Integer> reportScreenSwitch(@PathVariable Long id) {
        Long studentId = UserContext.getUserId();
        int count = examTakingService.reportScreenSwitch(id, studentId);
        return Result.success(count);
    }
}
