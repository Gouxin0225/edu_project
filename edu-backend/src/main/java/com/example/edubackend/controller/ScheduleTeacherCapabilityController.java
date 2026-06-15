package com.example.edubackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.ScheduleTeacherDispatchVO;
import com.example.edubackend.dto.ScheduleTeacherProfileConfigDTO;
import com.example.edubackend.dto.ScheduleTeacherSkillConfigDTO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IScheduleTeacherCapabilityService;
import com.example.edubackend.service.OperationAuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/schedule/teachers")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER"})
public class ScheduleTeacherCapabilityController {

    private final IScheduleTeacherCapabilityService scheduleTeacherCapabilityService;
    private final OperationAuditLogService auditLogService;

    @GetMapping
    public Result<Page<ScheduleTeacherDispatchVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dispatchStatus,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Byte canTravel,
            @RequestParam(required = false) Byte isCenterReserve) {
        return Result.success(scheduleTeacherCapabilityService.getTeacherProfiles(
                pageNum, pageSize, keyword, dispatchStatus, courseId, canTravel, isCenterReserve));
    }

    @PutMapping("/{teacherId}/profile")
    public Result<ScheduleTeacherDispatchVO> saveProfile(
            @PathVariable Long teacherId,
            @Valid @RequestBody ScheduleTeacherProfileConfigDTO dto) {
        dto.setTeacherId(teacherId);
        ScheduleTeacherDispatchVO vo = scheduleTeacherCapabilityService.saveProfile(dto);
        auditLogService.record("SCHEDULE_TEACHER_PROFILE", "SYS_USER", teacherId,
                "配置讲师派课资料");
        return Result.success(vo);
    }

    @PutMapping("/{teacherId}/skills")
    public Result<ScheduleTeacherDispatchVO> saveSkills(
            @PathVariable Long teacherId,
            @Valid @RequestBody ScheduleTeacherSkillConfigDTO dto) {
        dto.setTeacherId(teacherId);
        ScheduleTeacherDispatchVO vo = scheduleTeacherCapabilityService.saveTeacherSkills(dto);
        auditLogService.record("SCHEDULE_TEACHER_SKILL", "SYS_USER", teacherId,
                "配置讲师可授课程");
        return Result.success(vo);
    }

    @GetMapping("/available")
    public Result<List<ScheduleTeacherDispatchVO>> availableTeachers(
            @RequestParam Long courseId,
            @RequestParam(required = false) String skillLevel) {
        return Result.success(scheduleTeacherCapabilityService.getAvailableTeachersByCourse(courseId, skillLevel));
    }
}
