package com.example.edubackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.ScheduleAdvancedBatchDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentVO;
import com.example.edubackend.dto.ScheduleAdvancedBatchVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.ScheduleAdvancedBatchService;
import com.example.edubackend.service.ScheduleAdvancedBatchStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scheduling/advanced-batches")
@RequiredArgsConstructor
public class ScheduleAdvancedBatchController {

    private final ScheduleAdvancedBatchService advancedBatchService;
    private final ScheduleAdvancedBatchStudentService advancedBatchStudentService;

    @GetMapping
    @RequireRole({"ADMIN", "CENTER", "ASSISTANT"})
    public Result<Page<ScheduleAdvancedBatchVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String batchStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(advancedBatchService.getBatches(pageNum, pageSize, courseId, schoolName, batchStatus, keyword));
    }

    @PostMapping
    @RequireRole({"ADMIN", "CENTER"})
    public Result<ScheduleAdvancedBatchVO> create(@Valid @RequestBody ScheduleAdvancedBatchDTO dto) {
        return Result.success(advancedBatchService.createBatch(dto));
    }

    @GetMapping("/{id}/eligibility")
    @RequireRole({"ADMIN", "CENTER", "ASSISTANT"})
    public Result<List<ScheduleAdvancedBatchStudentVO>> eligibility(@PathVariable Long id) {
        return Result.success(advancedBatchStudentService.checkEligibility(id));
    }
}
