package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateClassDTO;
import com.example.edubackend.dto.ClassTeacherAssignDTO;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.ISysClassService;
import com.example.edubackend.service.ITeacherClassRelService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.ClassVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminClassController {

    private final ISysClassService sysClassService;

    @PostMapping("/class")
    @RequireRole("ADMIN")
    public Result<SysClass> createClass(@Valid @RequestBody CreateClassDTO dto) {
        SysClass sysClass = sysClassService.createClass(dto, UserContext.getUserId());
        return Result.success(sysClass);
    }

    @GetMapping("/class")
    @RequireRole("ADMIN")
    public Result<Page<ClassVO>> getClassPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) Long teacherId) {
        return Result.success(sysClassService.getClassPage(pageNum, pageSize, keyword, status, teacherId));
    }

    @PostMapping("/class/{id}/teacher")
    @RequireRole("ADMIN")
    public Result<Void> assignTeacher(@PathVariable Long id, @RequestBody ClassTeacherAssignDTO dto) {
        if (dto.getTeacherId() == null) {
            throw new BusinessException(400, "人员ID不能为空");
        }
        sysClassService.assignTeacher(id, dto.getTeacherId());
        return Result.success("分配成功");
    }

    @DeleteMapping("/class/{id}/teacher/{teacherId}")
    @RequireRole("ADMIN")
    public Result<Void> removeTeacher(@PathVariable Long id, @PathVariable Long teacherId) {
        sysClassService.removeTeacher(id, teacherId);
        return Result.success("移除成功");
    }
}
