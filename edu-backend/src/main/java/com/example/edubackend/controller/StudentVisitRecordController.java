package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateStudentVisitRecordDTO;
import com.example.edubackend.dto.StudentVisitRecordVO;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.entity.StudentVisitRecord;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.StudentVisitRecordMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student-visits")
@RequiredArgsConstructor
public class StudentVisitRecordController {

    private static final Set<String> VISIT_METHODS = Set.of("PHONE", "WECHAT", "OFFLINE", "ONLINE", "OTHER");
    private static final Set<String> VISIT_RESULTS = Set.of("REACHED", "UNREACHED", "NEED_FOLLOW", "RESOLVED");

    private final StudentVisitRecordMapper visitRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final SysClassMapper sysClassMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final ClassStudentRelMapper classStudentRelMapper;

    @GetMapping
    @RequireRole({"TEACHER", "ASSISTANT"})
    public Result<List<StudentVisitRecordVO>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId) {
        Set<Long> allowedClassIds = getAllowedClassIds();
        if (allowedClassIds.isEmpty()) {
            return Result.success(List.of());
        }

        LambdaQueryWrapper<StudentVisitRecord> wrapper = new LambdaQueryWrapper<StudentVisitRecord>()
                .orderByDesc(StudentVisitRecord::getVisitTime)
                .orderByDesc(StudentVisitRecord::getCreateTime);

        if (classId != null) {
            if (!allowedClassIds.contains(classId)) {
                throw new BusinessException(403, "无权查看该班级回访记录");
            }
            wrapper.eq(StudentVisitRecord::getClassId, classId);
        } else {
            wrapper.in(StudentVisitRecord::getClassId, allowedClassIds);
        }

        if (studentId != null) {
            SysUser student = assertAllowedStudent(studentId, allowedClassIds);
            wrapper.eq(StudentVisitRecord::getStudentId, student.getId());
        }

        List<StudentVisitRecord> records = visitRecordMapper.selectList(wrapper);
        return Result.success(toVOList(records));
    }

    @PostMapping
    @RequireRole({"TEACHER", "ASSISTANT"})
    public Result<Long> create(@Valid @RequestBody CreateStudentVisitRecordDTO dto) {
        assertValidVisitMeta(dto);
        Set<Long> allowedClassIds = getAllowedClassIds();
        SysUser student = assertAllowedStudent(dto.getStudentId(), allowedClassIds);
        SysUser visitor = UserContext.getUser();

        StudentVisitRecord record = new StudentVisitRecord();
        record.setStudentId(student.getId());
        record.setClassId(resolveAllowedClassId(student, allowedClassIds));
        record.setVisitorId(visitor.getId());
        record.setVisitorRole(visitor.getRole());
        record.setVisitMethod(dto.getVisitMethod());
        record.setVisitResult(dto.getVisitResult());
        record.setContent(dto.getContent());
        record.setNextFollowTime(dto.getNextFollowTime());
        record.setVisitTime(dto.getVisitTime() == null ? LocalDateTime.now() : dto.getVisitTime());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        visitRecordMapper.insert(record);
        return Result.success(record.getId());
    }

    private void assertValidVisitMeta(CreateStudentVisitRecordDTO dto) {
        if (!VISIT_METHODS.contains(dto.getVisitMethod())) {
            throw new BusinessException(400, "回访方式不合法");
        }
        if (!VISIT_RESULTS.contains(dto.getVisitResult())) {
            throw new BusinessException(400, "回访结果不合法");
        }
    }

    private Set<Long> getAllowedClassIds() {
        return teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>()
                        .eq(TeacherClassRel::getTeacherId, UserContext.getUserId())
        ).stream()
                .map(TeacherClassRel::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private SysUser assertAllowedStudent(Long studentId, Set<Long> allowedClassIds) {
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(404, "学生不存在");
        }
        if (resolveAllowedClassId(student, allowedClassIds) == null) {
            throw new BusinessException(403, "只能操作自己负责班级的学生");
        }
        return student;
    }

    private Long resolveAllowedClassId(SysUser student, Set<Long> allowedClassIds) {
        if (allowedClassIds == null || allowedClassIds.isEmpty()) {
            return null;
        }
        if (student.getClassId() != null && allowedClassIds.contains(student.getClassId())) {
            return student.getClassId();
        }
        return classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, student.getId())
                        .eq(ClassStudentRel::getStatus, "ACTIVE")
                        .in(ClassStudentRel::getClassId, allowedClassIds)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(ClassStudentRel::getClassId)
                .orElse(null);
    }

    private List<StudentVisitRecordVO> toVOList(List<StudentVisitRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        Set<Long> studentIds = records.stream()
                .map(StudentVisitRecord::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> classIds = records.stream()
                .map(StudentVisitRecord::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> visitorIds = records.stream()
                .map(StudentVisitRecord::getVisitorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, SysUser> students = studentIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, studentIds))
                        .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysClass> classes = classIds.isEmpty()
                ? Map.of()
                : sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>().in(SysClass::getId, classIds))
                        .stream().collect(Collectors.toMap(SysClass::getId, Function.identity()));
        Map<Long, SysUser> visitors = visitorIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, visitorIds))
                        .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));

        return records.stream().map(record -> {
            StudentVisitRecordVO vo = new StudentVisitRecordVO();
            vo.setId(record.getId());
            vo.setStudentId(record.getStudentId());
            vo.setClassId(record.getClassId());
            vo.setVisitorId(record.getVisitorId());
            vo.setVisitorRole(record.getVisitorRole());
            vo.setVisitMethod(record.getVisitMethod());
            vo.setVisitResult(record.getVisitResult());
            vo.setContent(record.getContent());
            vo.setNextFollowTime(record.getNextFollowTime());
            vo.setVisitTime(record.getVisitTime());
            vo.setCreateTime(record.getCreateTime());

            SysUser student = students.get(record.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
                vo.setStudentUsername(student.getUsername());
            }
            SysClass clazz = classes.get(record.getClassId());
            if (clazz != null) {
                vo.setClassName(clazz.getClassName());
            }
            SysUser visitor = visitors.get(record.getVisitorId());
            if (visitor != null) {
                vo.setVisitorName(visitor.getRealName());
            }
            return vo;
        }).toList();
    }
}
