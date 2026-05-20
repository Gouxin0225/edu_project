package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.*;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService studentService;

    @GetMapping("/dashboard")
    @RequireRole({"STUDENT"})
    public Result<StudentDashboardVO> getDashboard() {
        Long studentId = UserContext.getUserId();
        StudentDashboardVO dashboard = studentService.getDashboard(studentId);
        return Result.success(dashboard);
    }

    @GetMapping("/mistakes")
    @RequireRole({"STUDENT"})
    public Result<java.util.List<MistakeVO>> getMistakeList(
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam(required = false) String questionType) {
        Long studentId = UserContext.getUserId();
        java.util.List<MistakeVO> mistakes = studentService.getMistakeList(studentId, knowledgePoint, questionType);
        return Result.success(mistakes);
    }

    @PutMapping("/mistakes/{questionId}/master")
    @RequireRole({"STUDENT"})
    public Result<Void> markMistakeMastered(@PathVariable Long questionId) {
        Long studentId = UserContext.getUserId();
        studentService.markMistakeMastered(studentId, questionId);
        return Result.success("已标记为攻克");
    }

    @PostMapping("/mistakes/challenge")
    @RequireRole({"STUDENT"})
    public Result<ChallengeVO> challenge(@Valid @RequestBody ChallengeDTO dto) {
        Long studentId = UserContext.getUserId();
        ChallengeVO challenge = studentService.challenge(studentId, dto);
        return Result.success(challenge);
    }
}