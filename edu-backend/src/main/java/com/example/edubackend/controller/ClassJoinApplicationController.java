package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ClassJoinApplicationVO;
import com.example.edubackend.dto.RejectJoinApplicationDTO;
import com.example.edubackend.entity.ClassJoinApplication;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ClassJoinApplicationMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/class-join-applications")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "ASSISTANT"})
public class ClassJoinApplicationController {

    private final ClassJoinApplicationMapper applicationMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysUserMapper sysUserMapper;
    private final SysClassMapper sysClassMapper;

    @GetMapping
    public Result<List<ClassJoinApplicationVO>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        Set<Long> allowedClassIds = allowedClassIds();
        if (allowedClassIds.isEmpty()) {
            return Result.success(List.of());
        }
        if (classId != null && !allowedClassIds.contains(classId)) {
            throw new BusinessException(403, "无权查看该班级申请");
        }

        LambdaQueryWrapper<ClassJoinApplication> wrapper = new LambdaQueryWrapper<ClassJoinApplication>()
                .in(ClassJoinApplication::getClassId, allowedClassIds)
                .orderByDesc(ClassJoinApplication::getApplyTime);
        if (classId != null) {
            wrapper.eq(ClassJoinApplication::getClassId, classId);
        }
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(ClassJoinApplication::getStatus, status.trim().toUpperCase());
        }

        List<ClassJoinApplication> applications = applicationMapper.selectList(wrapper);
        return Result.success(toVOList(applications));
    }

    @PutMapping("/{id}/approve")
    @Transactional
    public Result<Void> approve(@PathVariable Long id) {
        ClassJoinApplication application = assertPendingAndAllowed(id);
        SysUser student = sysUserMapper.selectById(application.getStudentId());
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(404, "学生不存在");
        }

        upsertStudentClassRel(application.getClassId(), student.getId(), "SELF_APPLY");
        if (student.getClassId() == null) {
            student.setClassId(application.getClassId());
            sysUserMapper.updateById(student);
        }

        application.setStatus("APPROVED");
        application.setAuditTime(LocalDateTime.now());
        application.setAuditorId(UserContext.getUserId());
        applicationMapper.updateById(application);
        return Result.success("已通过申请");
    }

    @PutMapping("/{id}/reject")
    @Transactional
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) RejectJoinApplicationDTO dto) {
        ClassJoinApplication application = assertPendingAndAllowed(id);
        application.setStatus("REJECTED");
        application.setAuditTime(LocalDateTime.now());
        application.setAuditorId(UserContext.getUserId());
        application.setRejectReason(dto == null || !StringUtils.hasText(dto.getRejectReason())
                ? "班主任已拒绝"
                : dto.getRejectReason().trim());
        applicationMapper.updateById(application);
        return Result.success("已拒绝申请");
    }

    private ClassJoinApplication assertPendingAndAllowed(Long id) {
        ClassJoinApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(404, "申请不存在");
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException(400, "只能审核待处理申请");
        }
        if (!allowedClassIds().contains(application.getClassId())) {
            throw new BusinessException(403, "无权审核该班级申请");
        }
        return application;
    }

    private void upsertStudentClassRel(Long classId, Long studentId, String source) {
        ClassStudentRel rel = classStudentRelMapper.selectOne(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getClassId, classId)
                .eq(ClassStudentRel::getStudentId, studentId)
                .last("LIMIT 1"));
        if (rel == null) {
            rel = new ClassStudentRel();
            rel.setClassId(classId);
            rel.setStudentId(studentId);
            rel.setStatus("ACTIVE");
            rel.setJoinSource(source);
            rel.setCreatedBy(UserContext.getUserId());
            rel.setJoinTime(LocalDateTime.now());
            classStudentRelMapper.insert(rel);
            return;
        }
        if (!"ACTIVE".equals(rel.getStatus())) {
            rel.setStatus("ACTIVE");
            rel.setJoinSource(source);
            rel.setCreatedBy(UserContext.getUserId());
            rel.setJoinTime(LocalDateTime.now());
            rel.setLeaveTime(null);
            classStudentRelMapper.updateById(rel);
        }
    }

    private Set<Long> allowedClassIds() {
        String role = UserContext.getUser().getRole();
        if ("ADMIN".equals(role)) {
            return sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>()
                            .eq(SysClass::getIsDeleted, (byte) 0))
                    .stream()
                    .map(SysClass::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
        }
        return teacherClassRelMapper.selectList(new LambdaQueryWrapper<TeacherClassRel>()
                        .eq(TeacherClassRel::getTeacherId, UserContext.getUserId()))
                .stream()
                .map(TeacherClassRel::getClassId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private List<ClassJoinApplicationVO> toVOList(List<ClassJoinApplication> applications) {
        if (applications.isEmpty()) {
            return List.of();
        }
        Set<Long> classIds = applications.stream()
                .map(ClassJoinApplication::getClassId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> studentIds = applications.stream()
                .map(ClassJoinApplication::getStudentId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> auditorIds = applications.stream()
                .map(ClassJoinApplication::getAuditorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, SysClass> classes = classIds.isEmpty() ? Map.of() : sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>().in(SysClass::getId, classIds))
                .stream().collect(Collectors.toMap(SysClass::getId, Function.identity()));
        Map<Long, SysUser> students = studentIds.isEmpty() ? Map.of() : sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, studentIds))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysUser> auditors = auditorIds.isEmpty() ? Map.of() : sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, auditorIds))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));

        return applications.stream().map(application -> {
            ClassJoinApplicationVO vo = new ClassJoinApplicationVO();
            vo.setId(application.getId());
            vo.setClassId(application.getClassId());
            vo.setStudentId(application.getStudentId());
            vo.setStatus(application.getStatus());
            vo.setApplyTime(application.getApplyTime());
            vo.setAuditTime(application.getAuditTime());
            vo.setAuditorId(application.getAuditorId());
            vo.setRejectReason(application.getRejectReason());
            SysClass sysClass = classes.get(application.getClassId());
            if (sysClass != null) {
                vo.setClassName(sysClass.getClassName());
            }
            SysUser student = students.get(application.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
                vo.setUsername(student.getUsername());
                vo.setPhone(student.getPhone());
                vo.setSchoolName(student.getSchoolName());
            }
            if (application.getAuditorId() != null) {
                SysUser auditor = auditors.get(application.getAuditorId());
                if (auditor != null) {
                    vo.setAuditorName(auditor.getRealName());
                }
            }
            return vo;
        }).toList();
    }
}
