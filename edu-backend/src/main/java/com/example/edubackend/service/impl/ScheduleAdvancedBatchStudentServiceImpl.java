package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentImportDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentVO;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.entity.ScheduleAdvancedBatch;
import com.example.edubackend.entity.ScheduleAdvancedBatchStudent;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.ScheduleAdvancedBatchMapper;
import com.example.edubackend.mapper.ScheduleAdvancedBatchStudentMapper;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.OperationAuditLogService;
import com.example.edubackend.service.ScheduleAdvancedBatchStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleAdvancedBatchStudentServiceImpl implements ScheduleAdvancedBatchStudentService {

    private static final List<String> REQUIRED_COURSE_CODES = List.of(
            "HCIA", "PYTHON", "RHCSA", "WEB", "MYSQL", "HCIP", "RHCE"
    );
    private static final String ELIGIBLE = "ELIGIBLE";
    private static final String INELIGIBLE = "INELIGIBLE";
    private static final String UNCHECKED = "UNCHECKED";

    private final ScheduleAdvancedBatchMapper batchMapper;
    private final ScheduleAdvancedBatchStudentMapper batchStudentMapper;
    private final ScheduleCampusGradeMapper campusGradeMapper;
    private final ScheduleCampusCourseProgressMapper progressMapper;
    private final ScheduleCourseMapper courseMapper;
    private final SysUserMapper sysUserMapper;
    private final SysClassMapper sysClassMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final OperationAuditLogService auditLogService;

    @Override
    public List<ScheduleAdvancedBatchStudentVO> listStudents(Long batchId) {
        ScheduleAdvancedBatch batch = getBatch(batchId);
        List<ScheduleAdvancedBatchStudent> records = batchStudentMapper.selectList(new LambdaQueryWrapper<ScheduleAdvancedBatchStudent>()
                .eq(ScheduleAdvancedBatchStudent::getBatchId, batch.getId())
                .orderByDesc(ScheduleAdvancedBatchStudent::getCreateTime));
        return toVOList(batch, records);
    }

    @Override
    @Transactional
    public List<ScheduleAdvancedBatchStudentVO> importStudents(Long batchId, ScheduleAdvancedBatchStudentImportDTO dto) {
        ScheduleAdvancedBatch batch = getBatch(batchId);
        List<Long> studentIds = distinctIds(dto.getStudentIds());
        if (studentIds.isEmpty()) {
            throw new BusinessException(400, "学生ID列表不能为空");
        }
        Map<Long, SysUser> studentMap = loadStudents(studentIds);
        if (studentMap.size() != studentIds.size()) {
            throw new BusinessException(404, "存在不存在或非学生账号");
        }

        List<ScheduleAdvancedBatchStudent> saved = new ArrayList<>();
        for (Long studentId : studentIds) {
            ScheduleAdvancedBatchStudent record = batchStudentMapper.selectOne(new LambdaQueryWrapper<ScheduleAdvancedBatchStudent>()
                    .eq(ScheduleAdvancedBatchStudent::getBatchId, batch.getId())
                    .eq(ScheduleAdvancedBatchStudent::getStudentId, studentId));
            if (record == null) {
                record = new ScheduleAdvancedBatchStudent();
                record.setBatchId(batch.getId());
                record.setStudentId(studentId);
                record.setJoinStatus("PENDING");
                record.setCreatedBy(UserContext.getUserId());
            }
            StudentPlacement placement = resolvePlacement(studentMap.get(studentId));
            record.setClassId(placement.classId());
            record.setCampusGradeId(placement.campusGradeId());
            applyEligibility(record, checkGradeEligibility(placement.campusGradeId()));
            if (record.getId() == null) {
                batchStudentMapper.insert(record);
            } else {
                batchStudentMapper.updateById(record);
            }
            saved.add(record);
        }
        auditLogService.record("IMPORT", "SCHEDULE_ADVANCED_BATCH_STUDENT", batch.getId(),
                "录入高级课批次学生名单，批次 " + batch.getBatchName() + "，人数 " + saved.size());
        return toVOList(batch, saved);
    }

    @Override
    @Transactional
    public List<ScheduleAdvancedBatchStudentVO> checkEligibility(Long batchId) {
        ScheduleAdvancedBatch batch = getBatch(batchId);
        List<ScheduleAdvancedBatchStudent> records = batchStudentMapper.selectList(new LambdaQueryWrapper<ScheduleAdvancedBatchStudent>()
                .eq(ScheduleAdvancedBatchStudent::getBatchId, batch.getId()));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> studentMap = loadStudents(records.stream()
                .map(ScheduleAdvancedBatchStudent::getStudentId)
                .collect(Collectors.toSet()));
        for (ScheduleAdvancedBatchStudent record : records) {
            SysUser student = studentMap.get(record.getStudentId());
            if (student == null) {
                applyEligibility(record, EligibilityResult.ineligible(List.of("学生不存在")));
            } else {
                StudentPlacement placement = resolvePlacement(student);
                record.setClassId(placement.classId());
                record.setCampusGradeId(placement.campusGradeId());
                applyEligibility(record, checkGradeEligibility(placement.campusGradeId()));
            }
            batchStudentMapper.updateById(record);
        }
        auditLogService.record("CHECK", "SCHEDULE_ADVANCED_BATCH_STUDENT", batch.getId(),
                "重新校验高级课批次学生资格，批次 " + batch.getBatchName());
        return toVOList(batch, records);
    }

    private EligibilityResult checkGradeEligibility(Long campusGradeId) {
        if (campusGradeId == null) {
            return EligibilityResult.ineligible(List.of("未匹配到校区年级"));
        }
        Map<String, ScheduleCourse> requiredCourseMap = courseMapper.selectList(new LambdaQueryWrapper<ScheduleCourse>()
                        .in(ScheduleCourse::getCourseCode, REQUIRED_COURSE_CODES)
                        .eq(ScheduleCourse::getStatus, "ACTIVE")
                        .eq(ScheduleCourse::getIsDeleted, (byte) 0))
                .stream()
                .collect(Collectors.toMap(ScheduleCourse::getCourseCode, Function.identity(), (a, b) -> a));

        List<String> missingCourses = new ArrayList<>();
        for (String code : REQUIRED_COURSE_CODES) {
            ScheduleCourse course = requiredCourseMap.get(code);
            if (course == null) {
                missingCourses.add(code);
                continue;
            }
            ScheduleCampusCourseProgress progress = progressMapper.selectOne(new LambdaQueryWrapper<ScheduleCampusCourseProgress>()
                    .eq(ScheduleCampusCourseProgress::getCampusGradeId, campusGradeId)
                    .eq(ScheduleCampusCourseProgress::getCourseId, course.getId()));
            if (progress == null || !"COMPLETED".equals(progress.getProgressStatus())) {
                missingCourses.add(StringUtils.hasText(course.getCourseName()) ? course.getCourseName() : code);
            }
        }
        return missingCourses.isEmpty()
                ? EligibilityResult.passed()
                : EligibilityResult.ineligible(missingCourses);
    }

    private void applyEligibility(ScheduleAdvancedBatchStudent record, EligibilityResult result) {
        record.setEligibilityStatus(result.eligible() ? ELIGIBLE : INELIGIBLE);
        record.setEligibilityCheckedTime(LocalDateTime.now());
        record.setIneligibleReason(result.eligible() ? null : String.join("、", result.missingCourses()));
    }

    private StudentPlacement resolvePlacement(SysUser student) {
        Long classId = student.getClassId();
        if (classId == null) {
            ClassStudentRel activeRel = classStudentRelMapper.selectOne(new LambdaQueryWrapper<ClassStudentRel>()
                    .eq(ClassStudentRel::getStudentId, student.getId())
                    .eq(ClassStudentRel::getStatus, "ACTIVE")
                    .orderByDesc(ClassStudentRel::getJoinTime)
                    .last("limit 1"));
            if (activeRel != null) {
                classId = activeRel.getClassId();
            }
        }
        if (classId == null) {
            return new StudentPlacement(null, null);
        }
        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null) {
            return new StudentPlacement(classId, null);
        }
        ScheduleCampusGrade grade = campusGradeMapper.selectOne(new LambdaQueryWrapper<ScheduleCampusGrade>()
                .eq(ScheduleCampusGrade::getClassId, classId)
                .eq(ScheduleCampusGrade::getIsDeleted, (byte) 0)
                .last("limit 1"));
        if (grade == null && StringUtils.hasText(sysClass.getSchoolName())) {
            grade = campusGradeMapper.selectOne(new LambdaQueryWrapper<ScheduleCampusGrade>()
                    .eq(ScheduleCampusGrade::getSchoolName, sysClass.getSchoolName().trim())
                    .eq(ScheduleCampusGrade::getStatus, "ACTIVE")
                    .eq(ScheduleCampusGrade::getIsDeleted, (byte) 0)
                    .orderByDesc(ScheduleCampusGrade::getCreateTime)
                    .last("limit 1"));
        }
        return new StudentPlacement(classId, grade == null ? null : grade.getId());
    }

    private ScheduleAdvancedBatch getBatch(Long batchId) {
        if (batchId == null) {
            throw new BusinessException(400, "高级课批次ID不能为空");
        }
        ScheduleAdvancedBatch batch = batchMapper.selectById(batchId);
        if (batch == null || Byte.valueOf((byte) 1).equals(batch.getIsDeleted())) {
            throw new BusinessException(404, "高级课批次不存在");
        }
        return batch;
    }

    private Map<Long, SysUser> loadStudents(Iterable<Long> studentIds) {
        List<Long> ids = new ArrayList<>();
        studentIds.forEach(ids::add);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, ids)
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, (byte) 0))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private List<ScheduleAdvancedBatchStudentVO> toVOList(ScheduleAdvancedBatch batch, List<ScheduleAdvancedBatchStudent> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Set<Long> studentIds = records.stream().map(ScheduleAdvancedBatchStudent::getStudentId).collect(Collectors.toSet());
        Set<Long> classIds = records.stream().map(ScheduleAdvancedBatchStudent::getClassId).filter(id -> id != null).collect(Collectors.toSet());
        Set<Long> gradeIds = records.stream().map(ScheduleAdvancedBatchStudent::getCampusGradeId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, SysUser> studentMap = studentIds.isEmpty() ? Map.of() : sysUserMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysClass> classMap = classIds.isEmpty() ? Map.of() : sysClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(SysClass::getId, Function.identity()));
        Map<Long, ScheduleCampusGrade> gradeMap = gradeIds.isEmpty() ? Map.of() : campusGradeMapper.selectBatchIds(gradeIds).stream()
                .collect(Collectors.toMap(ScheduleCampusGrade::getId, Function.identity()));
        return records.stream()
                .map(record -> toVO(batch, record, studentMap.get(record.getStudentId()),
                        classMap.get(record.getClassId()), gradeMap.get(record.getCampusGradeId())))
                .toList();
    }

    private ScheduleAdvancedBatchStudentVO toVO(ScheduleAdvancedBatch batch, ScheduleAdvancedBatchStudent record,
                                                SysUser student, SysClass sysClass, ScheduleCampusGrade grade) {
        ScheduleAdvancedBatchStudentVO vo = new ScheduleAdvancedBatchStudentVO();
        vo.setId(record.getId());
        vo.setBatchId(record.getBatchId());
        vo.setBatchName(batch.getBatchName());
        vo.setStudentId(record.getStudentId());
        vo.setStudentName(student == null ? null : student.getRealName());
        vo.setClassId(record.getClassId());
        vo.setClassName(sysClass == null ? null : sysClass.getClassName());
        vo.setCampusGradeId(record.getCampusGradeId());
        vo.setGradeName(grade == null ? null : grade.getGradeName());
        vo.setJoinStatus(record.getJoinStatus());
        vo.setEligibilityStatus(record.getEligibilityStatus());
        vo.setEligible(ELIGIBLE.equals(record.getEligibilityStatus()));
        vo.setMissingCourses(parseMissingCourses(record.getIneligibleReason()));
        vo.setEligibilityCheckedTime(record.getEligibilityCheckedTime());
        vo.setIneligibleReason(record.getIneligibleReason());
        vo.setJoinedTime(record.getJoinedTime());
        vo.setExitTime(record.getExitTime());
        vo.setRemark(record.getRemark());
        vo.setCreatedBy(record.getCreatedBy());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private List<String> parseMissingCourses(String reason) {
        if (!StringUtils.hasText(reason)) {
            return List.of();
        }
        return List.of(reason.split("、"));
    }

    private record StudentPlacement(Long classId, Long campusGradeId) {
    }

    private record EligibilityResult(boolean eligible, List<String> missingCourses) {
        private static EligibilityResult passed() {
            return new EligibilityResult(true, List.of());
        }

        private static EligibilityResult ineligible(List<String> missingCourses) {
            return new EligibilityResult(false, missingCourses);
        }
    }
}
