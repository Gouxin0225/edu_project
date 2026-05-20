package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.StudentClassVO;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/classes")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentClassController {

    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;

    @GetMapping("/available")
    public Result<List<StudentClassVO>> availableClasses() {
        SysUser student = UserContext.getUser();
        List<SysClass> classes = sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .eq(SysClass::getStatus, (byte) 1)
                .eq(SysClass::getIsDeleted, (byte) 0)
                .and(wrapper -> wrapper.eq(SysClass::getAllowStudentApply, (byte) 1).or().isNull(SysClass::getAllowStudentApply))
                .orderByDesc(SysClass::getCreateTime));

        if (StringUtils.hasText(student.getSchoolName())) {
            String schoolName = student.getSchoolName().trim();
            classes = classes.stream()
                    .filter(c -> !StringUtils.hasText(c.getSchoolName()) || schoolName.equals(c.getSchoolName().trim()))
                    .toList();
        }

        return Result.success(buildClassVOList(student, classes));
    }

    @GetMapping("/my")
    public Result<List<StudentClassVO>> myClasses() {
        SysUser student = UserContext.getUser();
        Set<Long> classIds = activeClassIds(student);
        if (classIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<SysClass> classes = sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .in(SysClass::getId, classIds)
                .eq(SysClass::getIsDeleted, (byte) 0)
                .orderByDesc(SysClass::getCreateTime));
        return Result.success(buildClassVOList(student, classes));
    }

    @PostMapping("/{classId}/apply")
    @Transactional
    public Result<Void> apply(@PathVariable Long classId) {
        SysUser student = UserContext.getUser();
        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null || (sysClass.getIsDeleted() != null && sysClass.getIsDeleted() == 1)) {
            throw new BusinessException(404, "班级不存在");
        }
        if (sysClass.getStatus() == null || sysClass.getStatus() != 1) {
            throw new BusinessException(400, "该班级不是进行中状态");
        }
        if (sysClass.getAllowStudentApply() != null && sysClass.getAllowStudentApply() == 0) {
            throw new BusinessException(400, "该班级未开放学生申请");
        }
        if (StringUtils.hasText(sysClass.getSchoolName())
                && StringUtils.hasText(student.getSchoolName())
                && !sysClass.getSchoolName().trim().equals(student.getSchoolName().trim())) {
            throw new BusinessException(403, "只能申请加入本学校的班级");
        }
        if (activeClassIds(student).contains(classId)) {
            throw new BusinessException(400, "你已在该班级中");
        }
        Long pendingCount = classJoinApplicationMapper.selectCount(new LambdaQueryWrapper<ClassJoinApplication>()
                .eq(ClassJoinApplication::getClassId, classId)
                .eq(ClassJoinApplication::getStudentId, student.getId())
                .eq(ClassJoinApplication::getStatus, "PENDING"));
        if (pendingCount > 0) {
            throw new BusinessException(400, "该班级已有待审核申请");
        }

        ClassJoinApplication application = new ClassJoinApplication();
        application.setClassId(classId);
        application.setStudentId(student.getId());
        application.setStatus("PENDING");
        application.setApplyTime(LocalDateTime.now());
        classJoinApplicationMapper.insert(application);
        return Result.success("申请已提交，等待班主任审核");
    }

    private List<StudentClassVO> buildClassVOList(SysUser student, List<SysClass> classes) {
        Set<Long> joinedClassIds = activeClassIds(student);
        Map<Long, ClassJoinApplication> applicationMap = latestApplications(student.getId());
        Map<Long, List<SysUser>> assignedUsers = assignedUsersByClass(classes.stream().map(SysClass::getId).toList());

        return classes.stream().map(c -> {
            StudentClassVO vo = new StudentClassVO();
            vo.setClassId(c.getId());
            vo.setClassName(c.getClassName());
            vo.setStatus(c.getStatus());
            vo.setSchoolName(c.getSchoolName());
            vo.setAllowStudentApply(c.getAllowStudentApply() == null || c.getAllowStudentApply() == 1);
            vo.setJoined(joinedClassIds.contains(c.getId()));

            ClassJoinApplication application = applicationMap.get(c.getId());
            vo.setApplicationStatus(application == null ? null : application.getStatus());
            vo.setApplyTime(application == null ? null : application.getApplyTime());
            vo.setAuditTime(application == null ? null : application.getAuditTime());
            vo.setRejectReason(application == null ? null : application.getRejectReason());

            List<SysUser> users = assignedUsers.getOrDefault(c.getId(), List.of());
            vo.setTeacherNames(users.stream().filter(u -> "TEACHER".equals(u.getRole())).map(SysUser::getRealName).toList());
            vo.setAssistantNames(users.stream().filter(u -> "ASSISTANT".equals(u.getRole())).map(SysUser::getRealName).toList());
            return vo;
        }).toList();
    }

    private Set<Long> activeClassIds(SysUser student) {
        Set<Long> ids = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, student.getId())
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getClassId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (student.getClassId() != null) {
            ids.add(student.getClassId());
        }
        return ids;
    }

    private Map<Long, ClassJoinApplication> latestApplications(Long studentId) {
        return classJoinApplicationMapper.selectList(new LambdaQueryWrapper<ClassJoinApplication>()
                        .eq(ClassJoinApplication::getStudentId, studentId)
                        .orderByDesc(ClassJoinApplication::getApplyTime))
                .stream()
                .collect(Collectors.toMap(ClassJoinApplication::getClassId, Function.identity(), (first, ignored) -> first));
    }

    private Map<Long, List<SysUser>> assignedUsersByClass(List<Long> classIds) {
        classIds = classIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (classIds.isEmpty()) {
            return Map.of();
        }
        List<TeacherClassRel> rels = teacherClassRelMapper.selectList(new LambdaQueryWrapper<TeacherClassRel>()
                .in(TeacherClassRel::getClassId, classIds));
        Set<Long> userIds = rels.stream()
                .map(TeacherClassRel::getTeacherId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getIsDeleted, (byte) 0))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return rels.stream()
                .sorted(Comparator.comparing(TeacherClassRel::getId))
                .filter(rel -> users.containsKey(rel.getTeacherId()))
                .collect(Collectors.groupingBy(TeacherClassRel::getClassId,
                        Collectors.mapping(rel -> users.get(rel.getTeacherId()), Collectors.toList())));
    }
}
