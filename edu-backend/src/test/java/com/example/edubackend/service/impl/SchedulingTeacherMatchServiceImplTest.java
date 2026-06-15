package com.example.edubackend.service.impl;

import com.example.edubackend.dto.SchedulingMatchTeachersDTO;
import com.example.edubackend.dto.SchedulingMatchTeachersVO;
import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
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
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulingTeacherMatchServiceImplTest {

    @Test
    void matchTeachersStopsWhenCourseCannotSchedule() {
        TestContext context = newContext(false);
        SchedulingMatchTeachersDTO dto = dto();

        assertThatThrownBy(() -> context.service.matchTeachers(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("课程暂不可安排");
    }

    @Test
    void matchTeachersFiltersUnavailableTravelAndConflictThenScoresCandidates() {
        TestContext context = newContext(true);
        when(context.skillMapper.selectList(any())).thenReturn(List.of(
                skill(11L, "MAIN"),
                skill(12L, "TEACHABLE"),
                skill(13L, "MAIN"),
                skill(14L, "MAIN")
        ));
        when(context.profileMapper.selectList(any()))
                .thenReturn(List.of(
                        profile(11L, "AVAILABLE", (byte) 1, (byte) 0),
                        profile(12L, "AVAILABLE", (byte) 1, (byte) 0),
                        profile(13L, "PAUSED", (byte) 1, (byte) 0),
                        profile(14L, "AVAILABLE", (byte) 0, (byte) 0)
                ))
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(context.userMapper.selectList(any())).thenReturn(List.of(
                teacher(11L, "teacher11"),
                teacher(12L, "teacher12"),
                teacher(13L, "teacher13"),
                teacher(14L, "teacher14")
        ));
        ScheduleDispatchPlan conflictPlan = plan(31L, "CONFIRMED", LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 5));
        ScheduleDispatchPlan recentPlan = plan(32L, "COMPLETED", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 21));
        when(context.planMapper.selectList(any()))
                .thenReturn(List.of(conflictPlan))
                .thenReturn(List.of(recentPlan));
        when(context.planTeacherMapper.selectList(any()))
                .thenReturn(List.of(planTeacher(31L, 12L)))
                .thenReturn(List.of(planTeacher(32L, 11L)));
        when(context.reserveMapper.selectList(any())).thenReturn(List.of());

        SchedulingMatchTeachersVO result = context.service.matchTeachers(dto());

        assertThat(result.getSuggestedTeacherCount()).isEqualTo(2);
        assertThat(result.getTeacherCandidates())
                .extracting(SchedulingMatchTeachersVO.TeacherCandidate::getTeacherId)
                .containsExactly(11L);
        assertThat(result.getTeacherCandidates().get(0).getScore()).isEqualTo(90);
        assertThat(result.getRecommendedTeacherIds()).containsExactly(11L);
        assertThat(result.getWarnings()).containsExactly("可推荐讲师数量不足，建议讲师数 2，当前可推荐 1");
    }

    private TestContext newContext(boolean canSchedule) {
        ScheduleCampusCourseProgressMapper progressMapper = mock(ScheduleCampusCourseProgressMapper.class);
        ScheduleCampusGradeMapper gradeMapper = mock(ScheduleCampusGradeMapper.class);
        ScheduleCourseMapper courseMapper = mock(ScheduleCourseMapper.class);
        ScheduleTeacherSkillMapper skillMapper = mock(ScheduleTeacherSkillMapper.class);
        ScheduleTeacherProfileMapper profileMapper = mock(ScheduleTeacherProfileMapper.class);
        ScheduleDispatchPlanMapper planMapper = mock(ScheduleDispatchPlanMapper.class);
        ScheduleDispatchPlanTeacherMapper planTeacherMapper = mock(ScheduleDispatchPlanTeacherMapper.class);
        ScheduleDirectionReserveMapper reserveMapper = mock(ScheduleDirectionReserveMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SchedulingPrerequisiteService prerequisiteService = mock(SchedulingPrerequisiteService.class);

        ScheduleCampusCourseProgress progress = new ScheduleCampusCourseProgress();
        progress.setId(1000L);
        progress.setCampusGradeId(100L);
        progress.setCourseId(10L);
        progress.setExpectedStudentCount(35);
        when(progressMapper.selectById(1000L)).thenReturn(progress);

        ScheduleCampusGrade grade = new ScheduleCampusGrade();
        grade.setId(100L);
        grade.setSchoolName("北京校区");
        grade.setGradeName("2026春");
        when(gradeMapper.selectById(100L)).thenReturn(grade);

        ScheduleCourse course = new ScheduleCourse();
        course.setId(10L);
        course.setCourseCode("HCIA");
        course.setCourseName("HCIA");
        course.setCourseDirection("NETWORK");
        course.setCourseLevel("BASIC");
        course.setTeacherCapacity(20);
        when(courseMapper.selectById(10L)).thenReturn(course);

        SchedulingPrerequisiteResultVO prerequisite = canSchedule
                ? SchedulingPrerequisiteResultVO.passed()
                : SchedulingPrerequisiteResultVO.blocked(List.of("前置课程未完成: HCIA"));
        when(prerequisiteService.check(any(), any(), any())).thenReturn(prerequisite);

        SchedulingTeacherMatchServiceImpl service = new SchedulingTeacherMatchServiceImpl(
                progressMapper,
                gradeMapper,
                courseMapper,
                skillMapper,
                profileMapper,
                planMapper,
                planTeacherMapper,
                reserveMapper,
                userMapper,
                prerequisiteService
        );
        return new TestContext(service, skillMapper, profileMapper, planMapper, planTeacherMapper, reserveMapper, userMapper);
    }

    private SchedulingMatchTeachersDTO dto() {
        SchedulingMatchTeachersDTO dto = new SchedulingMatchTeachersDTO();
        dto.setCampusCourseProgressId(1000L);
        dto.setPlannedStartDate(LocalDate.of(2026, 7, 1));
        dto.setPlannedEndDate(LocalDate.of(2026, 7, 7));
        return dto;
    }

    private ScheduleTeacherSkill skill(Long teacherId, String skillLevel) {
        ScheduleTeacherSkill skill = new ScheduleTeacherSkill();
        skill.setId(teacherId + 100);
        skill.setTeacherId(teacherId);
        skill.setCourseId(10L);
        skill.setSkillLevel(skillLevel);
        skill.setStatus("ACTIVE");
        return skill;
    }

    private ScheduleTeacherProfile profile(Long teacherId, String status, Byte canTravel, Byte reserve) {
        ScheduleTeacherProfile profile = new ScheduleTeacherProfile();
        profile.setTeacherId(teacherId);
        profile.setDispatchStatus(status);
        profile.setCanTravel(canTravel);
        profile.setIsCenterReserve(reserve);
        profile.setStatus("ACTIVE");
        return profile;
    }

    private SysUser teacher(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setRole("TEACHER");
        user.setIsDeleted((byte) 0);
        return user;
    }

    private ScheduleDispatchPlan plan(Long id, String status, LocalDate start, LocalDate end) {
        ScheduleDispatchPlan plan = new ScheduleDispatchPlan();
        plan.setId(id);
        plan.setPlanStatus(status);
        plan.setExpectedStartDate(start);
        plan.setExpectedEndDate(end);
        plan.setIsDeleted((byte) 0);
        return plan;
    }

    private ScheduleDispatchPlanTeacher planTeacher(Long planId, Long teacherId) {
        ScheduleDispatchPlanTeacher rel = new ScheduleDispatchPlanTeacher();
        rel.setPlanId(planId);
        rel.setTeacherId(teacherId);
        rel.setAssignStatus("CONFIRMED");
        return rel;
    }

    private record TestContext(SchedulingTeacherMatchServiceImpl service,
                               ScheduleTeacherSkillMapper skillMapper,
                               ScheduleTeacherProfileMapper profileMapper,
                               ScheduleDispatchPlanMapper planMapper,
                               ScheduleDispatchPlanTeacherMapper planTeacherMapper,
                               ScheduleDirectionReserveMapper reserveMapper,
                               SysUserMapper userMapper) {
    }
}
