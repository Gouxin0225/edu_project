package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleCampusCourseProgressVO;
import com.example.edubackend.dto.ScheduleCampusProgressInitDTO;
import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.service.IScheduleCampusProgressService;
import com.example.edubackend.service.SchedulingPrerequisiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleCampusProgressServiceImpl implements IScheduleCampusProgressService {

    private static final Set<String> PROGRESS_STATUSES = Set.of(
            "NOT_STARTED", "PLANNED", "IN_PROGRESS", "COMPLETED", "NEED_MAKEUP", "CANCELED"
    );

    private final ScheduleCampusGradeMapper scheduleCampusGradeMapper;
    private final ScheduleCampusCourseProgressMapper scheduleCampusCourseProgressMapper;
    private final ScheduleCourseMapper scheduleCourseMapper;
    private final SchedulingPrerequisiteService schedulingPrerequisiteService;

    @Override
    public Page<ScheduleCampusCourseProgressVO> getProgressPage(Integer pageNum, Integer pageSize, String schoolName,
                                                                Long campusGradeId, Long courseId, String progressStatus,
                                                                String keyword) {
        String allowedSchoolName = resolveAllowedSchoolName(schoolName);
        return queryProgress(pageNum, pageSize, allowedSchoolName, campusGradeId, courseId, progressStatus, keyword);
    }

    @Override
    public Page<ScheduleCampusCourseProgressVO> getMySchoolProgress(Integer pageNum, Integer pageSize, Long campusGradeId,
                                                                    Long courseId, String progressStatus, String keyword) {
        SysUser user = requireLoginUser();
        if (!StringUtils.hasText(user.getSchoolName())) {
            throw new BusinessException(400, "当前用户未配置所属校区");
        }
        return queryProgress(pageNum, pageSize, user.getSchoolName().trim(), campusGradeId, courseId, progressStatus, keyword);
    }

    @Override
    @Transactional
    public List<ScheduleCampusCourseProgressVO> initGradeCourses(ScheduleCampusProgressInitDTO dto) {
        String schoolName = required(dto.getSchoolName(), "校区名称不能为空");
        assertCanOperateSchool(schoolName);
        ScheduleCampusGrade grade = resolveGrade(dto, schoolName);

        Set<Long> courseIds = dto.getCourses().stream()
                .map(ScheduleCampusProgressInitDTO.CourseProgressItem::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, ScheduleCourse> courseMap = loadCourseMap(courseIds);
        if (courseMap.size() != courseIds.size()) {
            throw new BusinessException(404, "存在不存在的课程");
        }

        for (ScheduleCampusProgressInitDTO.CourseProgressItem item : dto.getCourses()) {
            ScheduleCampusCourseProgress progress = scheduleCampusCourseProgressMapper.selectOne(
                    new LambdaQueryWrapper<ScheduleCampusCourseProgress>()
                            .eq(ScheduleCampusCourseProgress::getCampusGradeId, grade.getId())
                            .eq(ScheduleCampusCourseProgress::getCourseId, item.getCourseId()));
            if (progress == null) {
                progress = new ScheduleCampusCourseProgress();
                progress.setCampusGradeId(grade.getId());
                progress.setCourseId(item.getCourseId());
                progress.setProgressPercent(BigDecimal.ZERO);
            }
            progress.setExpectedStudentCount(nonNegative(item.getExpectedStudentCount(), "预计学生人数不能小于0"));
            progress.setProgressStatus(StringUtils.hasText(item.getProgressStatus())
                    ? normalizeProgressStatus(item.getProgressStatus())
                    : defaultString(progress.getProgressStatus(), "NOT_STARTED"));
            progress.setRemark(StringUtils.hasText(item.getRemark()) ? item.getRemark().trim() : null);
            if (progress.getId() == null) {
                scheduleCampusCourseProgressMapper.insert(progress);
            } else {
                scheduleCampusCourseProgressMapper.updateById(progress);
            }
        }
        return listGradeProgress(grade.getId());
    }

    @Override
    @Transactional
    public ScheduleCampusCourseProgressVO updateExpectedStudentCount(Long id, Integer expectedStudentCount) {
        ScheduleCampusCourseProgress progress = getProgressForOperation(id);
        progress.setExpectedStudentCount(nonNegative(expectedStudentCount, "预计学生人数不能小于0"));
        scheduleCampusCourseProgressMapper.updateById(progress);
        return toVO(progress);
    }

    @Override
    @Transactional
    public ScheduleCampusCourseProgressVO updateRemark(Long id, String remark) {
        ScheduleCampusCourseProgress progress = getProgressForOperation(id);
        progress.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        scheduleCampusCourseProgressMapper.updateById(progress);
        return toVO(progress);
    }

    @Override
    @Transactional
    public ScheduleCampusCourseProgressVO updateStatus(Long id, String progressStatus) {
        ScheduleCampusCourseProgress progress = getProgressForOperation(id);
        progress.setProgressStatus(normalizeProgressStatus(progressStatus));
        if ("COMPLETED".equals(progress.getProgressStatus())) {
            progress.setProgressPercent(new BigDecimal("100.00"));
            progress.setCompletionConfirmedBy(UserContext.getUserId());
            progress.setCompletionConfirmedTime(LocalDateTime.now());
        }
        scheduleCampusCourseProgressMapper.updateById(progress);
        return toVO(progress);
    }

    private Page<ScheduleCampusCourseProgressVO> queryProgress(Integer pageNum, Integer pageSize, String schoolName,
                                                               Long campusGradeId, Long courseId, String progressStatus,
                                                               String keyword) {
        List<Long> gradeIds = loadGradeIds(schoolName, campusGradeId, keyword);
        if ((StringUtils.hasText(schoolName) || campusGradeId != null || StringUtils.hasText(keyword)) && gradeIds.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }
        Page<ScheduleCampusCourseProgress> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<ScheduleCampusCourseProgress> wrapper = new LambdaQueryWrapper<>();
        if (!gradeIds.isEmpty()) {
            wrapper.in(ScheduleCampusCourseProgress::getCampusGradeId, gradeIds);
        }
        if (courseId != null) {
            wrapper.eq(ScheduleCampusCourseProgress::getCourseId, courseId);
        }
        if (StringUtils.hasText(progressStatus)) {
            wrapper.eq(ScheduleCampusCourseProgress::getProgressStatus, normalizeProgressStatus(progressStatus));
        }
        wrapper.orderByDesc(ScheduleCampusCourseProgress::getUpdateTime);
        Page<ScheduleCampusCourseProgress> result = scheduleCampusCourseProgressMapper.selectPage(page, wrapper);

        Page<ScheduleCampusCourseProgressVO> voPage = new Page<>(page.getCurrent(), page.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(toVOList(result.getRecords()));
        return voPage;
    }

    private List<Long> loadGradeIds(String schoolName, Long campusGradeId, String keyword) {
        LambdaQueryWrapper<ScheduleCampusGrade> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(schoolName)) {
            wrapper.eq(ScheduleCampusGrade::getSchoolName, schoolName.trim());
        }
        if (campusGradeId != null) {
            wrapper.eq(ScheduleCampusGrade::getId, campusGradeId);
        }
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(ScheduleCampusGrade::getGradeName, trimmed)
                    .or()
                    .like(ScheduleCampusGrade::getSchoolName, trimmed));
        }
        wrapper.eq(ScheduleCampusGrade::getIsDeleted, (byte) 0);
        return scheduleCampusGradeMapper.selectList(wrapper).stream()
                .map(ScheduleCampusGrade::getId)
                .toList();
    }

    private ScheduleCampusGrade resolveGrade(ScheduleCampusProgressInitDTO dto, String schoolName) {
        if (dto.getCampusGradeId() != null) {
            ScheduleCampusGrade grade = scheduleCampusGradeMapper.selectById(dto.getCampusGradeId());
            if (grade == null) {
                throw new BusinessException(404, "校区年级不存在");
            }
            assertCanOperateSchool(grade.getSchoolName());
            return grade;
        }
        String gradeName = required(dto.getGradeName(), "年级名称不能为空");
        ScheduleCampusGrade grade = scheduleCampusGradeMapper.selectOne(new LambdaQueryWrapper<ScheduleCampusGrade>()
                .eq(ScheduleCampusGrade::getSchoolName, schoolName)
                .eq(ScheduleCampusGrade::getGradeName, gradeName)
                .eq(ScheduleCampusGrade::getIsDeleted, (byte) 0));
        if (grade == null) {
            grade = new ScheduleCampusGrade();
            grade.setSchoolName(schoolName);
            grade.setGradeName(gradeName);
            grade.setStatus("ACTIVE");
        }
        grade.setClassId(dto.getClassId());
        grade.setMajorDirection(StringUtils.hasText(dto.getMajorDirection()) ? dto.getMajorDirection().trim() : grade.getMajorDirection());
        if (dto.getStudentCount() != null) {
            grade.setStudentCount(nonNegative(dto.getStudentCount(), "学生人数不能小于0"));
        } else if (grade.getStudentCount() == null) {
            grade.setStudentCount(0);
        }
        if (grade.getId() == null) {
            scheduleCampusGradeMapper.insert(grade);
        } else {
            scheduleCampusGradeMapper.updateById(grade);
        }
        return grade;
    }

    private ScheduleCampusCourseProgress getProgressForOperation(Long id) {
        if (id == null) {
            throw new BusinessException(400, "课程进度ID不能为空");
        }
        ScheduleCampusCourseProgress progress = scheduleCampusCourseProgressMapper.selectById(id);
        if (progress == null) {
            throw new BusinessException(404, "课程进度不存在");
        }
        ScheduleCampusGrade grade = scheduleCampusGradeMapper.selectById(progress.getCampusGradeId());
        if (grade == null) {
            throw new BusinessException(404, "校区年级不存在");
        }
        assertCanOperateSchool(grade.getSchoolName());
        return progress;
    }

    private String resolveAllowedSchoolName(String requestedSchoolName) {
        SysUser user = requireLoginUser();
        if (isGlobalRole(user.getRole())) {
            return StringUtils.hasText(requestedSchoolName) ? requestedSchoolName.trim() : null;
        }
        if (!StringUtils.hasText(user.getSchoolName())) {
            throw new BusinessException(400, "当前用户未配置所属校区");
        }
        String ownSchool = user.getSchoolName().trim();
        if (StringUtils.hasText(requestedSchoolName) && !ownSchool.equals(requestedSchoolName.trim())) {
            throw new BusinessException(403, "只能操作本校区数据");
        }
        return ownSchool;
    }

    private void assertCanOperateSchool(String schoolName) {
        SysUser user = requireLoginUser();
        if (isGlobalRole(user.getRole())) {
            return;
        }
        if (!StringUtils.hasText(user.getSchoolName()) || !user.getSchoolName().trim().equals(required(schoolName, "校区名称不能为空"))) {
            throw new BusinessException(403, "只能操作本校区数据");
        }
    }

    private boolean isGlobalRole(String role) {
        return "ADMIN".equals(role) || "CENTER".equals(role);
    }

    private SysUser requireLoginUser() {
        SysUser user = UserContext.getUser();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user;
    }

    private List<ScheduleCampusCourseProgressVO> listGradeProgress(Long campusGradeId) {
        return toVOList(scheduleCampusCourseProgressMapper.selectList(new LambdaQueryWrapper<ScheduleCampusCourseProgress>()
                .eq(ScheduleCampusCourseProgress::getCampusGradeId, campusGradeId)
                .orderByAsc(ScheduleCampusCourseProgress::getCourseId)));
    }

    private List<ScheduleCampusCourseProgressVO> toVOList(List<ScheduleCampusCourseProgress> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, ScheduleCampusGrade> gradeMap = scheduleCampusGradeMapper.selectBatchIds(
                        records.stream().map(ScheduleCampusCourseProgress::getCampusGradeId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ScheduleCampusGrade::getId, Function.identity()));
        Map<Long, ScheduleCourse> courseMap = loadCourseMap(
                records.stream().map(ScheduleCampusCourseProgress::getCourseId).collect(Collectors.toSet()));
        return records.stream()
                .map(progress -> toVO(progress, gradeMap.get(progress.getCampusGradeId()), courseMap.get(progress.getCourseId())))
                .toList();
    }

    private ScheduleCampusCourseProgressVO toVO(ScheduleCampusCourseProgress progress) {
        ScheduleCampusGrade grade = scheduleCampusGradeMapper.selectById(progress.getCampusGradeId());
        ScheduleCourse course = scheduleCourseMapper.selectById(progress.getCourseId());
        return toVO(progress, grade, course);
    }

    private ScheduleCampusCourseProgressVO toVO(ScheduleCampusCourseProgress progress, ScheduleCampusGrade grade, ScheduleCourse course) {
        ScheduleCampusCourseProgressVO vo = new ScheduleCampusCourseProgressVO();
        vo.setId(progress.getId());
        vo.setCampusGradeId(progress.getCampusGradeId());
        vo.setSchoolName(grade != null ? grade.getSchoolName() : null);
        vo.setGradeName(grade != null ? grade.getGradeName() : null);
        vo.setCourseId(progress.getCourseId());
        vo.setCourseCode(course != null ? course.getCourseCode() : null);
        vo.setCourseName(course != null ? course.getCourseName() : null);
        vo.setExpectedStudentCount(progress.getExpectedStudentCount());
        vo.setProgressStatus(progress.getProgressStatus());
        vo.setProgressPercent(progress.getProgressPercent());
        vo.setPlannedStartDate(progress.getPlannedStartDate());
        vo.setPlannedEndDate(progress.getPlannedEndDate());
        vo.setActualStartDate(progress.getActualStartDate());
        vo.setActualEndDate(progress.getActualEndDate());
        vo.setCurrentTeacherId(progress.getCurrentTeacherId());
        vo.setLastPlanId(progress.getLastPlanId());
        vo.setCompletionConfirmedBy(progress.getCompletionConfirmedBy());
        vo.setCompletionConfirmedTime(progress.getCompletionConfirmedTime());
        vo.setRemark(progress.getRemark());
        SchedulingPrerequisiteResultVO prerequisite = schedulingPrerequisiteService.check(null, progress.getCampusGradeId(), progress.getCourseId());
        vo.setCanSchedule(prerequisite.getCanSchedule());
        vo.setBlockedReasons(prerequisite.getBlockedReasons());
        vo.setCreateTime(progress.getCreateTime());
        vo.setUpdateTime(progress.getUpdateTime());
        return vo;
    }

    private Map<Long, ScheduleCourse> loadCourseMap(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        return scheduleCourseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(ScheduleCourse::getId, Function.identity()));
    }

    private String normalizeProgressStatus(String status) {
        String value = required(status, "课程状态不能为空").toUpperCase(Locale.ROOT);
        value = switch (value) {
            case "未上" -> "NOT_STARTED";
            case "已计划" -> "PLANNED";
            case "执行中" -> "IN_PROGRESS";
            case "已完成" -> "COMPLETED";
            case "需补课" -> "NEED_MAKEUP";
            case "已取消" -> "CANCELED";
            default -> value;
        };
        if (!PROGRESS_STATUSES.contains(value)) {
            throw new BusinessException(400, "课程状态不合法");
        }
        return value;
    }

    private Integer nonNegative(Integer value, String message) {
        int normalized = value == null ? 0 : value;
        if (normalized < 0) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Page<ScheduleCampusCourseProgressVO> emptyPage(Integer pageNum, Integer pageSize) {
        Page<ScheduleCampusCourseProgressVO> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        page.setTotal(0);
        page.setRecords(List.of());
        return page;
    }
}
