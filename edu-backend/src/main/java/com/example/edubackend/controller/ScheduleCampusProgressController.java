package com.example.edubackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.ScheduleCampusCourseProgressVO;
import com.example.edubackend.dto.ScheduleCampusProgressInitDTO;
import com.example.edubackend.dto.ScheduleCampusProgressUpdateDTO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IScheduleCampusProgressService;
import com.example.edubackend.service.OperationAuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/schedule/campus-progress")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER", "ASSISTANT"})
public class ScheduleCampusProgressController {

    private final IScheduleCampusProgressService scheduleCampusProgressService;
    private final OperationAuditLogService auditLogService;

    @GetMapping("/my")
    public Result<Page<ScheduleCampusCourseProgressVO>> mySchoolProgress(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long campusGradeId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String progressStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(scheduleCampusProgressService.getMySchoolProgress(
                pageNum, pageSize, campusGradeId, courseId, progressStatus, keyword));
    }

    @GetMapping
    public Result<Page<ScheduleCampusCourseProgressVO>> allProgress(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) Long campusGradeId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String progressStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(scheduleCampusProgressService.getProgressPage(
                pageNum, pageSize, schoolName, campusGradeId, courseId, progressStatus, keyword));
    }

    @PostMapping("/init")
    public Result<List<ScheduleCampusCourseProgressVO>> initGradeCourses(@Valid @RequestBody ScheduleCampusProgressInitDTO dto) {
        List<ScheduleCampusCourseProgressVO> records = scheduleCampusProgressService.initGradeCourses(dto);
        auditLogService.record("SCHEDULE_CAMPUS_PROGRESS_INIT", "SCHEDULE_CAMPUS_GRADE",
                dto.getCampusGradeId(), "初始化校区年级应上课程 " + dto.getSchoolName() + "/" + dto.getGradeName());
        return Result.success(records);
    }

    @PatchMapping("/{id}/expected-student-count")
    public Result<ScheduleCampusCourseProgressVO> updateExpectedStudentCount(
            @PathVariable Long id,
            @RequestBody ScheduleCampusProgressUpdateDTO dto) {
        ScheduleCampusCourseProgressVO vo = scheduleCampusProgressService.updateExpectedStudentCount(id, dto.getExpectedStudentCount());
        auditLogService.record("SCHEDULE_CAMPUS_PROGRESS_STUDENT_COUNT", "SCHEDULE_CAMPUS_COURSE_PROGRESS",
                id, "修改预计学生人数");
        return Result.success(vo);
    }

    @PatchMapping("/{id}/remark")
    public Result<ScheduleCampusCourseProgressVO> updateRemark(
            @PathVariable Long id,
            @RequestBody ScheduleCampusProgressUpdateDTO dto) {
        ScheduleCampusCourseProgressVO vo = scheduleCampusProgressService.updateRemark(id, dto.getRemark());
        auditLogService.record("SCHEDULE_CAMPUS_PROGRESS_REMARK", "SCHEDULE_CAMPUS_COURSE_PROGRESS",
                id, "修改课程进度备注");
        return Result.success(vo);
    }

    @PatchMapping("/{id}/status")
    public Result<ScheduleCampusCourseProgressVO> updateStatus(
            @PathVariable Long id,
            @RequestBody ScheduleCampusProgressUpdateDTO dto) {
        ScheduleCampusCourseProgressVO vo = scheduleCampusProgressService.updateStatus(id, dto.getProgressStatus());
        auditLogService.record("SCHEDULE_CAMPUS_PROGRESS_STATUS", "SCHEDULE_CAMPUS_COURSE_PROGRESS",
                id, "修改课程进度状态为 " + dto.getProgressStatus());
        return Result.success(vo);
    }
}
