package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentImportDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.ScheduleAdvancedBatchStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scheduling/advanced-batches/{batchId}/students")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER", "ASSISTANT"})
public class ScheduleAdvancedBatchStudentController {

    private final ScheduleAdvancedBatchStudentService advancedBatchStudentService;

    @GetMapping
    public Result<List<ScheduleAdvancedBatchStudentVO>> list(@PathVariable Long batchId) {
        return Result.success(advancedBatchStudentService.listStudents(batchId));
    }

    @PostMapping
    public Result<List<ScheduleAdvancedBatchStudentVO>> importStudents(
            @PathVariable Long batchId,
            @Valid @RequestBody ScheduleAdvancedBatchStudentImportDTO dto) {
        return Result.success(advancedBatchStudentService.importStudents(batchId, dto));
    }

    @PostMapping("/check-eligibility")
    public Result<List<ScheduleAdvancedBatchStudentVO>> checkEligibility(@PathVariable Long batchId) {
        return Result.success(advancedBatchStudentService.checkEligibility(batchId));
    }
}
