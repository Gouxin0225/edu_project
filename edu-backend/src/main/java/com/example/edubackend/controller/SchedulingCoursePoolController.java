package com.example.edubackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.SchedulingCoursePoolVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.SchedulingCoursePoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduling")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER"})
public class SchedulingCoursePoolController {

    private final SchedulingCoursePoolService schedulingCoursePoolService;

    @GetMapping("/course-pool")
    public Result<Page<SchedulingCoursePoolVO>> coursePool(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) Long campusGradeId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String keyword) {
        return Result.success(schedulingCoursePoolService.getCoursePool(
                pageNum, pageSize, schoolName, campusGradeId, courseId, keyword));
    }
}
