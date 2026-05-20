package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherClassController {

    private final SysClassMapper sysClassMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;

    @GetMapping("/my-classes")
    @RequireRole({"TEACHER", "ASSISTANT"})
    public Result<List<SysClass>> getMyClasses() {
        Long userId = UserContext.getUserId();
        
        List<Long> classIds = teacherClassRelMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TeacherClassRel>()
                        .eq(TeacherClassRel::getTeacherId, userId)
        ).stream()
                .map(TeacherClassRel::getClassId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (classIds.isEmpty()) {
            return Result.success(List.of());
        }

        List<SysClass> classes = sysClassMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysClass>()
                        .in(SysClass::getId, classIds)
                        .eq(SysClass::getStatus, (byte) 1)
        );

        return Result.success(classes);
    }
}
