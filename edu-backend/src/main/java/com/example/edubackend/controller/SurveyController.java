package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.*;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.ISurveyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SurveyController {

    private final ISurveyService surveyService;

    @PostMapping("/survey")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Long> createSurvey(@Valid @RequestBody CreateSurveyDTO dto) {
        Long creatorId = UserContext.getUserId();
        Long surveyId = surveyService.createSurvey(dto, creatorId);
        return Result.success(surveyId);
    }

    @PostMapping("/survey/{id}/publish")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Void> publishSurvey(@PathVariable Long id) {
        surveyService.publishSurvey(id);
        return Result.success("问卷发布成功");
    }

    @DeleteMapping("/survey/{id}")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Void> deleteSurvey(@PathVariable Long id) {
        surveyService.deleteSurvey(id);
        return Result.success("问卷已删除");
    }

    @GetMapping("/survey")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<SurveyListVO>> getSurveyList() {
        Long creatorId = UserContext.getUserId();
        String role = UserContext.getUser().getRole();
        List<SurveyListVO> list = surveyService.getSurveyList(creatorId, role);
        return Result.success(list);
    }

    @GetMapping("/student/survey")
    @RequireRole({"STUDENT"})
    public Result<List<StudentSurveyVO>> getStudentSurveyList() {
        Long studentId = UserContext.getUserId();
        List<StudentSurveyVO> list = surveyService.getStudentSurveyList(studentId);
        return Result.success(list);
    }

    @GetMapping("/survey/{id}/detail")
    @RequireRole({"STUDENT", "ADMIN", "TEACHER", "ASSISTANT"})
    public Result<SurveyDetailVO> getSurveyDetail(@PathVariable Long id) {
        SurveyDetailVO detail = surveyService.getSurveyDetail(id);
        return Result.success(detail);
    }

    @PostMapping("/survey/{id}/submit")
    @RequireRole({"STUDENT"})
    public Result<Void> submitSurvey(
            @PathVariable Long id,
            @Valid @RequestBody SubmitSurveyDTO dto,
            HttpServletRequest request) {
        Long studentId = UserContext.getUserId();
        String clientIp = getClientIp(request);
        dto.setSurveyId(id);
        surveyService.submitSurvey(studentId, dto, clientIp);
        return Result.success("问卷提交成功");
    }

    @GetMapping("/survey/{id}/statistics")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<SurveyStatisticsVO> getSurveyStatistics(@PathVariable Long id) {
        SurveyStatisticsVO statistics = surveyService.getSurveyStatistics(id);
        return Result.success(statistics);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
