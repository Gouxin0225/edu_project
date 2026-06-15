package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.SchedulingMatchTeachersDTO;
import com.example.edubackend.dto.SchedulingMatchTeachersVO;
import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.ScheduleDirectionReserve;
import com.example.edubackend.entity.ScheduleDispatchPlan;
import com.example.edubackend.entity.ScheduleDispatchPlanTeacher;
import com.example.edubackend.entity.ScheduleTeacherProfile;
import com.example.edubackend.entity.ScheduleTeacherSkill;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.ScheduleDirectionReserveMapper;
import com.example.edubackend.mapper.ScheduleDispatchPlanMapper;
import com.example.edubackend.mapper.ScheduleDispatchPlanTeacherMapper;
import com.example.edubackend.mapper.ScheduleTeacherProfileMapper;
import com.example.edubackend.mapper.ScheduleTeacherSkillMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.SchedulingPrerequisiteService;
import com.example.edubackend.service.SchedulingTeacherMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingTeacherMatchServiceImpl implements SchedulingTeacherMatchService {

    private static final Set<String> CONFLICT_PLAN_STATUSES = Set.of(
            "PENDING_CONFIRM", "CONFIRMED", "IN_PROGRESS", "待确认", "已确认", "执行中"
    );
    private static final Set<String> ACTIVE_ASSIGN_STATUSES = Set.of(
            "ASSIGNED", "CONFIRMED", "IN_PROGRESS", "CANDIDATE", "已指派", "已确认", "执行中"
    );

    private final ScheduleCampusCourseProgressMapper scheduleCampusCourseProgressMapper;
    private final ScheduleCampusGradeMapper scheduleCampusGradeMapper;
    private final ScheduleCourseMapper scheduleCourseMapper;
    private final ScheduleTeacherSkillMapper scheduleTeacherSkillMapper;
    private final ScheduleTeacherProfileMapper scheduleTeacherProfileMapper;
    private final ScheduleDispatchPlanMapper scheduleDispatchPlanMapper;
    private final ScheduleDispatchPlanTeacherMapper scheduleDispatchPlanTeacherMapper;
    private final ScheduleDirectionReserveMapper scheduleDirectionReserveMapper;
    private final SysUserMapper sysUserMapper;
    private final SchedulingPrerequisiteService schedulingPrerequisiteService;

    @Override
    public SchedulingMatchTeachersVO matchTeachers(SchedulingMatchTeachersDTO dto) {
        validateDateRange(dto.getPlannedStartDate(), dto.getPlannedEndDate());
        ScheduleCampusCourseProgress progress = getProgress(dto.getCampusCourseProgressId());
        ScheduleCampusGrade grade = getGrade(progress.getCampusGradeId());
        ScheduleCourse course = getCourse(progress.getCourseId());

        SchedulingPrerequisiteResultVO prerequisite = schedulingPrerequisiteService.check(
                null, progress.getCampusGradeId(), progress.getCourseId());
        if (!Boolean.TRUE.equals(prerequisite.getCanSchedule())) {
            throw new BusinessException(400, "课程暂不可安排: " + String.join("；", prerequisite.getBlockedReasons()));
        }

        SchedulingMatchTeachersVO vo = new SchedulingMatchTeachersVO();
        int suggestedTeacherCount = calculateSuggestedTeacherCount(progress.getExpectedStudentCount(), course.getTeacherCapacity());
        vo.setSuggestedTeacherCount(suggestedTeacherCount);

        List<ScheduleTeacherSkill> skills = scheduleTeacherSkillMapper.selectList(new LambdaQueryWrapper<ScheduleTeacherSkill>()
                .eq(ScheduleTeacherSkill::getCourseId, course.getId())
                .eq(ScheduleTeacherSkill::getStatus, "ACTIVE"));
        if (skills.isEmpty()) {
            vo.getWarnings().add("没有配置可授该课程的讲师");
            return vo;
        }

        Map<Long, ScheduleTeacherSkill> skillByTeacher = skills.stream()
                .collect(Collectors.toMap(ScheduleTeacherSkill::getTeacherId, Function.identity(), (a, b) -> a));
        Set<Long> teacherIds = skillByTeacher.keySet();
        Map<Long, ScheduleTeacherProfile> profileMap = loadProfiles(teacherIds);
        Map<Long, SysUser> teacherMap = loadTeachers(teacherIds);
        Set<Long> conflictTeacherIds = loadConflictTeacherIds(dto.getPlannedStartDate(), dto.getPlannedEndDate());
        Map<Long, Integer> recentDispatchCountMap = loadRecentDispatchCount(teacherIds, dto.getPlannedStartDate());

        List<SchedulingMatchTeachersVO.TeacherCandidate> candidates = new ArrayList<>();
        for (Long teacherId : teacherIds) {
            ScheduleTeacherSkill skill = skillByTeacher.get(teacherId);
            ScheduleTeacherProfile profile = profileMap.get(teacherId);
            SysUser teacher = teacherMap.get(teacherId);
            if (teacher == null) {
                continue;
            }
            if (profile == null || !"AVAILABLE".equals(profile.getDispatchStatus())) {
                continue;
            }
            if (!Byte.valueOf((byte) 1).equals(profile.getCanTravel())) {
                continue;
            }
            if (conflictTeacherIds.contains(teacherId)) {
                continue;
            }

            boolean reserveRisk = hasReserveRisk(profile, course, grade, teacherIds);
            int recentDispatchCount = recentDispatchCountMap.getOrDefault(teacherId, 0);
            SchedulingMatchTeachersVO.TeacherCandidate candidate = buildCandidate(
                    teacher, skill, profile, reserveRisk, recentDispatchCount);
            candidates.add(candidate);
        }

        candidates = candidates.stream()
                .sorted(Comparator.comparing(SchedulingMatchTeachersVO.TeacherCandidate::getScore).reversed()
                        .thenComparing(SchedulingMatchTeachersVO.TeacherCandidate::getTeacherId))
                .toList();
        vo.setTeacherCandidates(candidates);
        vo.setRecommendedTeacherIds(candidates.stream()
                .limit(suggestedTeacherCount)
                .map(SchedulingMatchTeachersVO.TeacherCandidate::getTeacherId)
                .toList());
        if (vo.getRecommendedTeacherIds().size() < suggestedTeacherCount) {
            vo.getWarnings().add("可推荐讲师数量不足，建议讲师数 " + suggestedTeacherCount + "，当前可推荐 "
                    + vo.getRecommendedTeacherIds().size());
        }
        if (candidates.stream().anyMatch(SchedulingMatchTeachersVO.TeacherCandidate::getReserveRisk)) {
            vo.getWarnings().add("部分候选讲师触发方向保底风险");
        }
        return vo;
    }

    private SchedulingMatchTeachersVO.TeacherCandidate buildCandidate(SysUser teacher, ScheduleTeacherSkill skill,
                                                                      ScheduleTeacherProfile profile,
                                                                      boolean reserveRisk,
                                                                      int recentDispatchCount) {
        SchedulingMatchTeachersVO.TeacherCandidate candidate = new SchedulingMatchTeachersVO.TeacherCandidate();
        candidate.setTeacherId(teacher.getId());
        candidate.setTeacherName(teacher.getRealName());
        candidate.setUsername(teacher.getUsername());
        candidate.setSkillId(skill.getId());
        candidate.setSkillLevel(skill.getSkillLevel());
        candidate.setDispatchStatus(profile.getDispatchStatus());
        candidate.setCanTravel(profile.getCanTravel());
        candidate.setIsCenterReserve(profile.getIsCenterReserve());
        candidate.setReserveRisk(reserveRisk);
        candidate.setRecentDispatchCount(recentDispatchCount);

        int score = 60;
        candidate.getReasons().add("基础分 +60");
        if ("MAIN".equals(skill.getSkillLevel())) {
            score += 20;
            candidate.getReasons().add("主讲 +20");
        } else if ("TEACHABLE".equals(skill.getSkillLevel())) {
            score += 10;
            candidate.getReasons().add("可讲 +10");
        }
        if (recentDispatchCount <= 0) {
            score += 20;
            candidate.getReasons().add("近30天无派课 +20");
        } else if (recentDispatchCount == 1) {
            score += 10;
            candidate.getReasons().add("近30天派课1次 +10");
        }
        if (reserveRisk) {
            score -= 30;
            candidate.getWarnings().add("触发方向保底风险");
        }
        candidate.setScore(score);
        return candidate;
    }

    private boolean hasReserveRisk(ScheduleTeacherProfile profile, ScheduleCourse course, ScheduleCampusGrade grade,
                                   Set<Long> capableTeacherIds) {
        if (!Byte.valueOf((byte) 1).equals(profile.getIsCenterReserve())) {
            return false;
        }
        List<ScheduleDirectionReserve> reserves = scheduleDirectionReserveMapper.selectList(
                new LambdaQueryWrapper<ScheduleDirectionReserve>()
                        .eq(ScheduleDirectionReserve::getDirectionCode, course.getCourseDirection())
                        .eq(ScheduleDirectionReserve::getStatus, "ACTIVE"));
        reserves = reserves.stream()
                .filter(reserve -> reserve.getCourseLevel() == null || course.getCourseLevel().equals(reserve.getCourseLevel()))
                .filter(reserve -> reserve.getSchoolName() == null || reserve.getSchoolName().equals(grade.getSchoolName()))
                .toList();
        if (reserves.isEmpty()) {
            return false;
        }
        int minTeacherCount = reserves.stream()
                .map(ScheduleDirectionReserve::getMinTeacherCount)
                .filter(count -> count != null && count > 0)
                .max(Integer::compareTo)
                .orElse(0);
        if (minTeacherCount <= 0) {
            return false;
        }
        long availableReserveCount = scheduleTeacherProfileMapper.selectList(new LambdaQueryWrapper<ScheduleTeacherProfile>()
                        .in(ScheduleTeacherProfile::getTeacherId, capableTeacherIds)
                        .eq(ScheduleTeacherProfile::getDispatchStatus, "AVAILABLE")
                        .eq(ScheduleTeacherProfile::getCanTravel, (byte) 1)
                        .eq(ScheduleTeacherProfile::getIsCenterReserve, (byte) 1)
                        .eq(ScheduleTeacherProfile::getStatus, "ACTIVE"))
                .stream()
                .filter(reserveProfile -> !reserveProfile.getTeacherId().equals(profile.getTeacherId()))
                .count();
        return availableReserveCount < minTeacherCount;
    }

    private Set<Long> loadConflictTeacherIds(LocalDate plannedStartDate, LocalDate plannedEndDate) {
        List<ScheduleDispatchPlan> conflictPlans = scheduleDispatchPlanMapper.selectList(
                new LambdaQueryWrapper<ScheduleDispatchPlan>()
                        .le(ScheduleDispatchPlan::getExpectedStartDate, plannedEndDate)
                        .ge(ScheduleDispatchPlan::getExpectedEndDate, plannedStartDate)
                        .in(ScheduleDispatchPlan::getPlanStatus, CONFLICT_PLAN_STATUSES)
                        .eq(ScheduleDispatchPlan::getIsDeleted, (byte) 0));
        if (conflictPlans.isEmpty()) {
            return Set.of();
        }
        List<Long> planIds = conflictPlans.stream().map(ScheduleDispatchPlan::getId).toList();
        return scheduleDispatchPlanTeacherMapper.selectList(new LambdaQueryWrapper<ScheduleDispatchPlanTeacher>()
                        .in(ScheduleDispatchPlanTeacher::getPlanId, planIds)
                        .in(ScheduleDispatchPlanTeacher::getAssignStatus, ACTIVE_ASSIGN_STATUSES))
                .stream()
                .map(ScheduleDispatchPlanTeacher::getTeacherId)
                .collect(Collectors.toSet());
    }

    private Map<Long, Integer> loadRecentDispatchCount(Set<Long> teacherIds, LocalDate plannedStartDate) {
        LocalDate start = plannedStartDate.minusDays(30);
        List<ScheduleDispatchPlan> recentPlans = scheduleDispatchPlanMapper.selectList(
                new LambdaQueryWrapper<ScheduleDispatchPlan>()
                        .ge(ScheduleDispatchPlan::getExpectedStartDate, start)
                        .le(ScheduleDispatchPlan::getExpectedStartDate, plannedStartDate)
                        .notIn(ScheduleDispatchPlan::getPlanStatus, List.of("CANCELED", "REJECTED", "已取消", "已驳回"))
                        .eq(ScheduleDispatchPlan::getIsDeleted, (byte) 0));
        if (recentPlans.isEmpty()) {
            return Map.of();
        }
        List<Long> planIds = recentPlans.stream().map(ScheduleDispatchPlan::getId).toList();
        return scheduleDispatchPlanTeacherMapper.selectList(new LambdaQueryWrapper<ScheduleDispatchPlanTeacher>()
                        .in(ScheduleDispatchPlanTeacher::getPlanId, planIds)
                        .in(ScheduleDispatchPlanTeacher::getTeacherId, teacherIds)
                        .in(ScheduleDispatchPlanTeacher::getAssignStatus, ACTIVE_ASSIGN_STATUSES))
                .stream()
                .collect(Collectors.groupingBy(ScheduleDispatchPlanTeacher::getTeacherId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private Map<Long, ScheduleTeacherProfile> loadProfiles(Set<Long> teacherIds) {
        return scheduleTeacherProfileMapper.selectList(new LambdaQueryWrapper<ScheduleTeacherProfile>()
                        .in(ScheduleTeacherProfile::getTeacherId, teacherIds)
                        .eq(ScheduleTeacherProfile::getStatus, "ACTIVE"))
                .stream()
                .collect(Collectors.toMap(ScheduleTeacherProfile::getTeacherId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, SysUser> loadTeachers(Set<Long> teacherIds) {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, teacherIds)
                        .eq(SysUser::getRole, "TEACHER")
                        .eq(SysUser::getIsDeleted, (byte) 0))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private ScheduleCampusCourseProgress getProgress(Long progressId) {
        if (progressId == null) {
            throw new BusinessException(400, "课程进度ID不能为空");
        }
        ScheduleCampusCourseProgress progress = scheduleCampusCourseProgressMapper.selectById(progressId);
        if (progress == null) {
            throw new BusinessException(404, "课程进度不存在");
        }
        return progress;
    }

    private ScheduleCampusGrade getGrade(Long gradeId) {
        ScheduleCampusGrade grade = scheduleCampusGradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BusinessException(404, "校区年级不存在");
        }
        return grade;
    }

    private ScheduleCourse getCourse(Long courseId) {
        ScheduleCourse course = scheduleCourseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
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
}
