package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.TeacherOptionVO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeacherOptionController {

    private final SysUserMapper sysUserMapper;

    @GetMapping("/teachers")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<TeacherOptionVO>> listTeachers() {
        List<TeacherOptionVO> teachers = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "TEACHER")
                        .eq(SysUser::getIsDeleted, (byte) 0)
                        .orderByAsc(SysUser::getRealName))
                .stream()
                .map(this::toVO)
                .toList();
        return Result.success(teachers);
    }

    private TeacherOptionVO toVO(SysUser user) {
        TeacherOptionVO vo = new TeacherOptionVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return vo;
    }
}
