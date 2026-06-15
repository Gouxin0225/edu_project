package com.example.edubackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.ScheduleDispatchPlanCreateDTO;
import com.example.edubackend.dto.ScheduleDispatchPlanVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.SchedulingDispatchPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduling/dispatch-plans")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER"})
public class SchedulingDispatchPlanController {

    private final SchedulingDispatchPlanService schedulingDispatchPlanService;

    @GetMapping
    public Result<Page<ScheduleDispatchPlanVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long campusGradeId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String planStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(schedulingDispatchPlanService.getDispatchPlans(
                pageNum, pageSize, campusGradeId, courseId, planStatus, keyword));
    }

    @PostMapping
    public Result<ScheduleDispatchPlanVO> create(@Valid @RequestBody ScheduleDispatchPlanCreateDTO dto) {
        return Result.success(schedulingDispatchPlanService.createDispatchPlan(dto));
    }

    @PutMapping("/{id}/confirm")
    public Result<ScheduleDispatchPlanVO> confirm(@PathVariable Long id) {
        return Result.success(schedulingDispatchPlanService.confirm(id));
    }

    @PutMapping("/{id}/start")
    public Result<ScheduleDispatchPlanVO> start(@PathVariable Long id) {
        return Result.success(schedulingDispatchPlanService.start(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<ScheduleDispatchPlanVO> cancel(@PathVariable Long id) {
        return Result.success(schedulingDispatchPlanService.cancel(id));
    }
}
