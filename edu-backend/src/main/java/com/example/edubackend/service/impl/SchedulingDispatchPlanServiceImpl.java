package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleDispatchPlanCreateDTO;
import com.example.edubackend.dto.ScheduleDispatchPlanTeacherVO;
import com.example.edubackend.dto.ScheduleDispatchPlanVO;
import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.ScheduleDispatchPlan;
import com.example.edubackend.entity.ScheduleDispatchPlanTeacher;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.ScheduleDispatchPlanMapper;
import com.example.edubackend.mapper.ScheduleDispatchPlanTeacherMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.OperationAuditLogService;
import com.example.edubackend.service.SchedulingDispatchPlanService;
import com.example.edubackend.service.SchedulingPrerequisiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingDispatchPlanServiceImpl implements SchedulingDispatchPlanService {

    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final Set<String> SCHEDULABLE_PROGRESS_STATUSES = Set.of("NOT_STARTED", "NEED_MAKEUP");

    private final ScheduleCampusCourseProgressMapper progressMapper;
    private final ScheduleCampusGradeMapper gradeMapper;
    private final ScheduleCourseMapper courseMapper;
    private final ScheduleDispatchPlanMapper planMapper;
    private final ScheduleDispatchPlanTeacherMapper planTeacherMapper;
    private final SysUserMapper sysUserMapper;
    private final SchedulingPrerequisiteService prerequisiteService;
    private final OperationAuditLogService auditLogService;

    @Override
    public Page<ScheduleDispatchPlanVO> getDispatchPlans(Integer pageNum, Integer pageSize, Long campusGradeId,
                                                         Long courseId, String planStatus, String keyword) {
        Page<ScheduleDispatchPlan> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<ScheduleDispatchPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleDispatchPlan::getIsDeleted, (byte) 0);
        if (campusGradeId != null) {
            wrapper.eq(ScheduleDispatchPlan::getCampusGradeId, campusGradeId);
        }
        if (courseId != null) {
            wrapper.eq(ScheduleDispatchPlan::getCourseId, courseId);
        }
        if (StringUtils.hasText(planStatus)) {
            wrapper.eq(ScheduleDispatchPlan::getPlanStatus, normalizePlanStatus(planStatus));
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(ScheduleDispatchPlan::getPlanNo, value)
                    .or()
                    .like(ScheduleDispatchPlan::getDispatchReason, value)
                    .or()
                    .like(ScheduleDispatchPlan::getRequirementDesc, value));
        }
        wrapper.orderByDesc(ScheduleDispatchPlan::getCreateTime);

        Page<ScheduleDispatchPlan> result = planMapper.selectPage(page, wrapper);
        Page<ScheduleDispatchPlanVO> voPage = new Page<>(page.getCurrent(), page.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(toVOList(result.getRecords()));
        return voPage;
    }

    @Override
    @Transactional
    public ScheduleDispatchPlanVO createDispatchPlan(ScheduleDispatchPlanCreateDTO dto) {
        validateDateRange(dto.getPlannedStartDate(), dto.getPlannedEndDate());
        ScheduleCampusCourseProgress progress = getProgress(dto.getCampusCourseProgressId());
        ScheduleCampusGrade grade = getGrade(progress.getCampusGradeId());
        ScheduleCourse course = getCourse(progress.getCourseId());
        validateCourseSchedulable(progress);

        SchedulingPrerequisiteResultVO prerequisite = prerequisiteService.check(null, progress.getCampusGradeId(), progress.getCourseId());
        if (!Boolean.TRUE.equals(prerequisite.getCanSchedule())) {
            throw new BusinessException(400, "课程暂不可安排: " + String.join("；", prerequisite.getBlockedReasons()));
        }

        List<Long> teacherIds = distinctTeacherIds(dto.getTeacherIds());
        int suggestedTeacherCount = calculateSuggestedTeacherCount(progress.getExpectedStudentCount(), course.getTeacherCapacity());
        String warning = teacherIds.size() < suggestedTeacherCount
                ? "派遣讲师数量低于建议讲师数，建议 " + suggestedTeacherCount + " 人，当前 " + teacherIds.size() + " 人"
                : null;

        ScheduleDispatchPlan plan = new ScheduleDispatchPlan();
        plan.setPlanNo(generatePlanNo());
        plan.setCampusGradeId(progress.getCampusGradeId());
        plan.setCourseId(progress.getCourseId());
        plan.setPlanType("NORMAL");
        plan.setPlanStatus(STATUS_PENDING_CONFIRM);
        plan.setPriority("NORMAL");
        plan.setExpectedStartDate(dto.getPlannedStartDate());
        plan.setExpectedEndDate(dto.getPlannedEndDate());
        plan.setDispatchReason(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        plan.setRequirementDesc("建议讲师数: " + suggestedTeacherCount);
        plan.setCreatedBy(UserContext.getUserId());
        plan.setSubmittedTime(LocalDateTime.now());
        plan.setIsDeleted((byte) 0);
        planMapper.insert(plan);

        for (Long teacherId : teacherIds) {
            ScheduleDispatchPlanTeacher rel = new ScheduleDispatchPlanTeacher();
            rel.setPlanId(plan.getId());
            rel.setTeacherId(teacherId);
            rel.setTeacherRole("MAIN");
            rel.setAssignStatus("CANDIDATE");
            planTeacherMapper.insert(rel);
        }

        progress.setProgressStatus("PLANNED");
        progress.setPlannedStartDate(dto.getPlannedStartDate());
        progress.setPlannedEndDate(dto.getPlannedEndDate());
        progress.setCurrentTeacherId(teacherIds.isEmpty() ? null : teacherIds.get(0));
        progress.setLastPlanId(plan.getId());
        progressMapper.updateById(progress);

        auditLogService.record("CREATE", "SCHEDULE_DISPATCH_PLAN", plan.getId(),
                "创建派遣计划 " + plan.getPlanNo() + "，校区年级 " + grade.getGradeName() + "，课程 " + course.getCourseName());
        ScheduleDispatchPlanVO vo = toVO(plan, grade, course, loadTeachers(List.of(plan.getId())).getOrDefault(plan.getId(), List.of()));
        vo.setSuggestedTeacherCount(suggestedTeacherCount);
        if (StringUtils.hasText(warning)) {
            vo.setWarning(warning);
            vo.getWarnings().add(warning);
        }
        return vo;
    }

    @Override
    @Transactional
    public ScheduleDispatchPlanVO confirm(Long id) {
        ScheduleDispatchPlan plan = getPlan(id);
        requireStatus(plan, STATUS_PENDING_CONFIRM, "只有待确认计划可以确认");
        plan.setPlanStatus(STATUS_CONFIRMED);
        plan.setApprovedBy(UserContext.getUserId());
        plan.setApprovedTime(LocalDateTime.now());
        planMapper.updateById(plan);
        updateTeacherAssignStatus(plan.getId(), STATUS_CONFIRMED, LocalDateTime.now());
        auditLogService.record("CONFIRM", "SCHEDULE_DISPATCH_PLAN", plan.getId(), "确认派遣计划 " + plan.getPlanNo());
        return toVO(plan);
    }

    @Override
    @Transactional
    public ScheduleDispatchPlanVO start(Long id) {
        ScheduleDispatchPlan plan = getPlan(id);
        requireStatus(plan, STATUS_CONFIRMED, "只有已确认计划可以开始执行");
        plan.setPlanStatus(STATUS_IN_PROGRESS);
        planMapper.updateById(plan);
        updateTeacherAssignStatus(plan.getId(), STATUS_IN_PROGRESS, null);

        ScheduleCampusCourseProgress progress = getProgressByPlan(plan);
        progress.setProgressStatus("IN_PROGRESS");
        progress.setActualStartDate(LocalDate.now());
        progress.setCurrentTeacherId(firstTeacherId(plan.getId()));
        progressMapper.updateById(progress);

        auditLogService.record("START", "SCHEDULE_DISPATCH_PLAN", plan.getId(), "开始执行派遣计划 " + plan.getPlanNo());
        return toVO(plan);
    }

    @Override
    @Transactional
    public ScheduleDispatchPlanVO cancel(Long id) {
        ScheduleDispatchPlan plan = getPlan(id);
        if (STATUS_CANCELED.equals(plan.getPlanStatus())) {
            throw new BusinessException(400, "计划已取消");
        }
        String previousStatus = plan.getPlanStatus();
        plan.setPlanStatus(STATUS_CANCELED);
        plan.setCancelReason("手动取消");
        planMapper.updateById(plan);
        updateTeacherAssignStatus(plan.getId(), STATUS_CANCELED, null);

        ScheduleCampusCourseProgress progress = getProgressByPlan(plan);
        progress.setProgressStatus(rollbackProgressStatus(previousStatus, progress));
        progress.setCurrentTeacherId(null);
        progress.setLastPlanId(null);
        if (STATUS_IN_PROGRESS.equals(previousStatus)) {
            progress.setActualEndDate(LocalDate.now());
        }
        progressMapper.updateById(progress);

        auditLogService.record("CANCEL", "SCHEDULE_DISPATCH_PLAN", plan.getId(), "取消派遣计划 " + plan.getPlanNo());
        return toVO(plan);
    }

    private void validateCourseSchedulable(ScheduleCampusCourseProgress progress) {
        String status = progress.getProgressStatus();
        if (!SCHEDULABLE_PROGRESS_STATUSES.contains(status)) {
            throw new BusinessException(400, "当前课程状态不可安排");
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException(400, "计划起止日期不能为空");
        }
        if (end.isBefore(start)) {
            throw new BusinessException(400, "计划结束日期不能早于开始日期");
        }
    }

    private Integer calculateSuggestedTeacherCount(Integer expectedStudentCount, Integer teacherCapacity) {
        if (expectedStudentCount == null || expectedStudentCount <= 0 || teacherCapacity == null || teacherCapacity <= 0) {
            return 1;
        }
        return (expectedStudentCount + teacherCapacity - 1) / teacherCapacity;
    }

    private List<Long> distinctTeacherIds(List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return List.of();
        }
        return teacherIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private ScheduleCampusCourseProgress getProgress(Long id) {
        if (id == null) {
            throw new BusinessException(400, "课程进度ID不能为空");
        }
        ScheduleCampusCourseProgress progress = progressMapper.selectById(id);
        if (progress == null) {
            throw new BusinessException(404, "课程进度不存在");
        }
        return progress;
    }

    private ScheduleCampusCourseProgress getProgressByPlan(ScheduleDispatchPlan plan) {
        ScheduleCampusCourseProgress progress = progressMapper.selectOne(new LambdaQueryWrapper<ScheduleCampusCourseProgress>()
                .eq(ScheduleCampusCourseProgress::getCampusGradeId, plan.getCampusGradeId())
                .eq(ScheduleCampusCourseProgress::getCourseId, plan.getCourseId())
                .eq(ScheduleCampusCourseProgress::getLastPlanId, plan.getId()));
        if (progress == null) {
            throw new BusinessException(404, "派遣计划关联课程进度不存在");
        }
        return progress;
    }

    private ScheduleCampusGrade getGrade(Long id) {
        ScheduleCampusGrade grade = gradeMapper.selectById(id);
        if (grade == null) {
            throw new BusinessException(404, "校区年级不存在");
        }
        return grade;
    }

    private ScheduleCourse getCourse(Long id) {
        ScheduleCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!"ACTIVE".equals(course.getStatus())) {
            throw new BusinessException(400, "课程已停用，不能安排");
        }
        return course;
    }

    private ScheduleDispatchPlan getPlan(Long id) {
        if (id == null) {
            throw new BusinessException(400, "派遣计划ID不能为空");
        }
        ScheduleDispatchPlan plan = planMapper.selectById(id);
        if (plan == null || Byte.valueOf((byte) 1).equals(plan.getIsDeleted())) {
            throw new BusinessException(404, "派遣计划不存在");
        }
        return plan;
    }

    private void requireStatus(ScheduleDispatchPlan plan, String requiredStatus, String message) {
        if (!requiredStatus.equals(plan.getPlanStatus())) {
            throw new BusinessException(400, message);
        }
    }

    private String rollbackProgressStatus(String previousPlanStatus, ScheduleCampusCourseProgress progress) {
        if (STATUS_IN_PROGRESS.equals(previousPlanStatus)
                || progress.getActualStartDate() != null
                || (progress.getProgressPercent() != null && progress.getProgressPercent().signum() > 0)) {
            return "NEED_MAKEUP";
        }
        return "NOT_STARTED";
    }

    private Long firstTeacherId(Long planId) {
        List<ScheduleDispatchPlanTeacher> teachers = planTeacherMapper.selectList(new LambdaQueryWrapper<ScheduleDispatchPlanTeacher>()
                .eq(ScheduleDispatchPlanTeacher::getPlanId, planId)
                .orderByAsc(ScheduleDispatchPlanTeacher::getId));
        return teachers.isEmpty() ? null : teachers.get(0).getTeacherId();
    }

    private void updateTeacherAssignStatus(Long planId, String assignStatus, LocalDateTime confirmTime) {
        ScheduleDispatchPlanTeacher update = new ScheduleDispatchPlanTeacher();
        update.setAssignStatus(assignStatus);
        if (confirmTime != null) {
            update.setConfirmTime(confirmTime);
        }
        planTeacherMapper.update(update, new LambdaQueryWrapper<ScheduleDispatchPlanTeacher>()
                .eq(ScheduleDispatchPlanTeacher::getPlanId, planId));
    }

    private String normalizePlanStatus(String status) {
        String value = status.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "待确认" -> STATUS_PENDING_CONFIRM;
            case "已确认" -> STATUS_CONFIRMED;
            case "执行中" -> STATUS_IN_PROGRESS;
            case "已取消" -> STATUS_CANCELED;
            default -> value;
        };
    }

    private String generatePlanNo() {
        return "DP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private List<ScheduleDispatchPlanVO> toVOList(List<ScheduleDispatchPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        Map<Long, ScheduleCampusGrade> gradeMap = gradeMapper.selectBatchIds(
                        plans.stream().map(ScheduleDispatchPlan::getCampusGradeId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ScheduleCampusGrade::getId, Function.identity()));
        Map<Long, ScheduleCourse> courseMap = courseMapper.selectBatchIds(
                        plans.stream().map(ScheduleDispatchPlan::getCourseId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ScheduleCourse::getId, Function.identity()));
        Map<Long, List<ScheduleDispatchPlanTeacherVO>> teacherMap = loadTeachers(plans.stream()
                .map(ScheduleDispatchPlan::getId)
                .toList());
        return plans.stream()
                .map(plan -> toVO(plan, gradeMap.get(plan.getCampusGradeId()), courseMap.get(plan.getCourseId()),
                        teacherMap.getOrDefault(plan.getId(), List.of())))
                .toList();
    }

    private ScheduleDispatchPlanVO toVO(ScheduleDispatchPlan plan) {
        return toVO(plan, getGrade(plan.getCampusGradeId()), getCourse(plan.getCourseId()),
                loadTeachers(List.of(plan.getId())).getOrDefault(plan.getId(), List.of()));
    }

    private ScheduleDispatchPlanVO toVO(ScheduleDispatchPlan plan, ScheduleCampusGrade grade, ScheduleCourse course,
                                        List<ScheduleDispatchPlanTeacherVO> teachers) {
        ScheduleDispatchPlanVO vo = new ScheduleDispatchPlanVO();
        vo.setId(plan.getId());
        vo.setPlanNo(plan.getPlanNo());
        vo.setCampusGradeId(plan.getCampusGradeId());
        vo.setGradeName(grade == null ? null : grade.getGradeName());
        vo.setCourseId(plan.getCourseId());
        vo.setCourseName(course == null ? null : course.getCourseName());
        vo.setPlanType(plan.getPlanType());
        vo.setPlanStatus(plan.getPlanStatus());
        vo.setPriority(plan.getPriority());
        vo.setExpectedStartDate(plan.getExpectedStartDate());
        vo.setExpectedEndDate(plan.getExpectedEndDate());
        vo.setPlannedStartDate(plan.getExpectedStartDate());
        vo.setPlannedEndDate(plan.getExpectedEndDate());
        vo.setDispatchReason(plan.getDispatchReason());
        vo.setRemark(plan.getDispatchReason());
        vo.setRequirementDesc(plan.getRequirementDesc());
        vo.setCreatedBy(plan.getCreatedBy());
        vo.setSubmittedTime(plan.getSubmittedTime());
        vo.setApprovedBy(plan.getApprovedBy());
        vo.setApprovedTime(plan.getApprovedTime());
        vo.setCancelReason(plan.getCancelReason());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());
        vo.setTeachers(teachers == null ? List.of() : teachers);
        return vo;
    }

    private Map<Long, List<ScheduleDispatchPlanTeacherVO>> loadTeachers(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        List<ScheduleDispatchPlanTeacher> relations = planTeacherMapper.selectList(new LambdaQueryWrapper<ScheduleDispatchPlanTeacher>()
                .in(ScheduleDispatchPlanTeacher::getPlanId, planIds)
                .orderByAsc(ScheduleDispatchPlanTeacher::getId));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(relations.stream()
                        .map(ScheduleDispatchPlanTeacher::getTeacherId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return relations.stream()
                .map(rel -> toTeacherVO(rel, userMap.get(rel.getTeacherId())))
                .collect(Collectors.groupingBy(ScheduleDispatchPlanTeacherVO::getPlanId));
    }

    private ScheduleDispatchPlanTeacherVO toTeacherVO(ScheduleDispatchPlanTeacher rel, SysUser teacher) {
        ScheduleDispatchPlanTeacherVO vo = new ScheduleDispatchPlanTeacherVO();
        vo.setId(rel.getId());
        vo.setPlanId(rel.getPlanId());
        vo.setTeacherId(rel.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getRealName());
        vo.setTeacherRole(rel.getTeacherRole());
        vo.setMatchScore(rel.getMatchScore());
        vo.setMatchReason(rel.getMatchReason());
        vo.setAssignStatus(rel.getAssignStatus());
        vo.setConfirmTime(rel.getConfirmTime());
        vo.setRejectReason(rel.getRejectReason());
        vo.setCreateTime(rel.getCreateTime());
        vo.setUpdateTime(rel.getUpdateTime());
        return vo;
    }
}
