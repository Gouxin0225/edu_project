package com.example.edubackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.ScheduleCourseDTO;
import com.example.edubackend.dto.ScheduleCourseVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IScheduleCourseService;
import com.example.edubackend.service.OperationAuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/schedule/courses")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER"})
public class ScheduleCourseController {

    private final IScheduleCourseService scheduleCourseService;
    private final OperationAuditLogService auditLogService;

    @GetMapping
    public Result<Page<ScheduleCourseVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String courseLevel,
            @RequestParam(required = false) String courseType,
            @RequestParam(required = false) String status) {
        return Result.success(scheduleCourseService.getCoursePage(pageNum, pageSize, keyword, courseLevel, courseType, status));
    }

    @PostMapping
    public Result<ScheduleCourseVO> create(@Valid @RequestBody ScheduleCourseDTO dto) {
        ScheduleCourseVO vo = scheduleCourseService.createCourse(dto);
        auditLogService.record("SCHEDULE_COURSE_CREATE", "SCHEDULE_COURSE", vo.getId(),
                "新增派课课程 " + vo.getCourseName());
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<ScheduleCourseVO> update(@PathVariable Long id, @Valid @RequestBody ScheduleCourseDTO dto) {
        ScheduleCourseVO vo = scheduleCourseService.updateCourse(id, dto);
        auditLogService.record("SCHEDULE_COURSE_UPDATE", "SCHEDULE_COURSE", id,
                "修改派课课程 " + vo.getCourseName());
        return Result.success(vo);
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        scheduleCourseService.updateStatus(id, status);
        auditLogService.record("SCHEDULE_COURSE_STATUS", "SCHEDULE_COURSE", id,
                "更新派课课程状态为 " + status);
        return Result.success("状态更新成功");
    }

    @PostMapping("/init-fixed")
    public Result<List<ScheduleCourseVO>> initFixedCourses() {
        List<ScheduleCourseVO> courses = scheduleCourseService.initFixedCourses();
        auditLogService.record("SCHEDULE_COURSE_INIT_FIXED", "SCHEDULE_COURSE", null,
                "初始化中心派课固定课程数据");
        return Result.success(courses);
    }
}
