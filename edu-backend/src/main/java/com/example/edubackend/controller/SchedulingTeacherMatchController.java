package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.SchedulingMatchTeachersDTO;
import com.example.edubackend.dto.SchedulingMatchTeachersVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.SchedulingTeacherMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduling")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "CENTER"})
public class SchedulingTeacherMatchController {

    private final SchedulingTeacherMatchService schedulingTeacherMatchService;

    @PostMapping("/match-teachers")
    public Result<SchedulingMatchTeachersVO> matchTeachers(@Valid @RequestBody SchedulingMatchTeachersDTO dto) {
        return Result.success(schedulingTeacherMatchService.matchTeachers(dto));
    }
}
