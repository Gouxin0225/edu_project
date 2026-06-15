package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleDispatchPlanCreateDTO;
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
import com.example.edubackend.service.SchedulingPrerequisiteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulingDispatchPlanServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void createDispatchPlanPersistsPlanTeachersAndReturnsWarningWhenTeacherCountIsLow() {
        TestContext context = newContext();
        List<ScheduleDispatchPlan> insertedPlans = new ArrayList<>();
        List<ScheduleDispatchPlanTeacher> insertedTeachers = new ArrayList<>();
        doAnswer(invocation -> {
            ScheduleDispatchPlan plan = invocation.getArgument(0);
            plan.setId(900L);
            insertedPlans.add(plan);
            return 1;
        }).when(context.planMapper).insert(any(ScheduleDispatchPlan.class));
        doAnswer(invocation -> {
            ScheduleDispatchPlanTeacher rel = invocation.getArgument(0);
            rel.setId((long) insertedTeachers.size() + 1);
            insertedTeachers.add(rel);
            return 1;
        }).when(context.planTeacherMapper).insert(any(ScheduleDispatchPlanTeacher.class));
        when(context.planTeacherMapper.selectList(any())).thenAnswer(invocation -> insertedTeachers);
        when(context.userMapper.selectBatchIds(any())).thenReturn(List.of(teacher(11L, "讲师A")));

        ScheduleDispatchPlanVO result = context.service.createDispatchPlan(createDto(List.of(11L)));

        assertThat(insertedPlans).hasSize(1);
        ScheduleDispatchPlan plan = insertedPlans.get(0);
        assertThat(plan.getPlanStatus()).isEqualTo("PENDING_CONFIRM");
        assertThat(plan.getCampusGradeId()).isEqualTo(100L);
        assertThat(plan.getCourseId()).isEqualTo(10L);
        assertThat(insertedTeachers).hasSize(1);
        assertThat(insertedTeachers.get(0).getTeacherId()).isEqualTo(11L);
        assertThat(context.progress.getProgressStatus()).isEqualTo("PLANNED");
        assertThat(context.progress.getLastPlanId()).isEqualTo(900L);
        assertThat(result.getSuggestedTeacherCount()).isEqualTo(2);
        assertThat(result.getWarning()).contains("建议 2 人，当前 1 人");
        assertThat(result.getTeachers()).extracting("teacherId").containsExactly(11L);
        verify(context.auditLogService).record(eq("CREATE"), eq("SCHEDULE_DISPATCH_PLAN"), eq(900L), any());
    }

    @Test
    void createDispatchPlanRejectsUnschedulableCourse() {
        TestContext context = newContext();
        context.progress.setProgressStatus("COMPLETED");

        assertThatThrownBy(() -> context.service.createDispatchPlan(createDto(List.of(11L, 12L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前课程状态不可安排");
    }

    @Test
    void confirmAndStartFollowStateFlowAndUpdateProgress() {
        TestContext context = newContext();
        ScheduleDispatchPlan plan = plan("PENDING_CONFIRM");
        when(context.planMapper.selectById(900L)).thenReturn(plan);
        when(context.progressMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(context.progress);
        ScheduleDispatchPlanTeacher rel = new ScheduleDispatchPlanTeacher();
        rel.setId(1L);
        rel.setPlanId(900L);
        rel.setTeacherId(11L);
        when(context.planTeacherMapper.selectList(any()))
                .thenReturn(List.of(rel))
                .thenReturn(List.of(rel));
        when(context.userMapper.selectBatchIds(any())).thenReturn(List.of(teacher(11L, "讲师A")));

        ScheduleDispatchPlanVO confirmed = context.service.confirm(900L);
        assertThat(confirmed.getPlanStatus()).isEqualTo("CONFIRMED");

        ScheduleDispatchPlanVO started = context.service.start(900L);
        assertThat(started.getPlanStatus()).isEqualTo("IN_PROGRESS");
        assertThat(context.progress.getProgressStatus()).isEqualTo("IN_PROGRESS");
        assertThat(context.progress.getCurrentTeacherId()).isEqualTo(11L);
        verify(context.auditLogService).record(eq("CONFIRM"), eq("SCHEDULE_DISPATCH_PLAN"), eq(900L), any());
        verify(context.auditLogService).record(eq("START"), eq("SCHEDULE_DISPATCH_PLAN"), eq(900L), any());
    }

    @Test
    void cancelConfirmedPlanRollsProgressBackToNotStarted() {
        TestContext context = newContext();
        ScheduleDispatchPlan plan = plan("CONFIRMED");
        when(context.planMapper.selectById(900L)).thenReturn(plan);
        when(context.progressMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(context.progress);
        when(context.planTeacherMapper.selectList(any())).thenReturn(List.of());

        ScheduleDispatchPlanVO canceled = context.service.cancel(900L);

        assertThat(canceled.getPlanStatus()).isEqualTo("CANCELED");
        assertThat(context.progress.getProgressStatus()).isEqualTo("NOT_STARTED");
        assertThat(context.progress.getLastPlanId()).isNull();
        verify(context.auditLogService).record(eq("CANCEL"), eq("SCHEDULE_DISPATCH_PLAN"), eq(900L), any());
    }

    @Test
    void cancelInProgressPlanRollsProgressBackToNeedMakeup() {
        TestContext context = newContext();
        ScheduleDispatchPlan plan = plan("IN_PROGRESS");
        context.progress.setProgressStatus("IN_PROGRESS");
        context.progress.setActualStartDate(LocalDate.of(2026, 7, 1));
        when(context.planMapper.selectById(900L)).thenReturn(plan);
        when(context.progressMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(context.progress);
        when(context.planTeacherMapper.selectList(any())).thenReturn(List.of());

        context.service.cancel(900L);

        assertThat(context.progress.getProgressStatus()).isEqualTo("NEED_MAKEUP");
        assertThat(context.progress.getActualEndDate()).isNotNull();
    }

    private TestContext newContext() {
        ScheduleCampusCourseProgressMapper progressMapper = mock(ScheduleCampusCourseProgressMapper.class);
        ScheduleCampusGradeMapper gradeMapper = mock(ScheduleCampusGradeMapper.class);
        ScheduleCourseMapper courseMapper = mock(ScheduleCourseMapper.class);
        ScheduleDispatchPlanMapper planMapper = mock(ScheduleDispatchPlanMapper.class);
        ScheduleDispatchPlanTeacherMapper planTeacherMapper = mock(ScheduleDispatchPlanTeacherMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SchedulingPrerequisiteService prerequisiteService = mock(SchedulingPrerequisiteService.class);
        OperationAuditLogService auditLogService = mock(OperationAuditLogService.class);

        ScheduleCampusCourseProgress progress = new ScheduleCampusCourseProgress();
        progress.setId(1000L);
        progress.setCampusGradeId(100L);
        progress.setCourseId(10L);
        progress.setExpectedStudentCount(35);
        progress.setProgressStatus("NOT_STARTED");
        when(progressMapper.selectById(1000L)).thenReturn(progress);

        ScheduleCampusGrade grade = new ScheduleCampusGrade();
        grade.setId(100L);
        grade.setSchoolName("北京校区");
        grade.setGradeName("2026春");
        when(gradeMapper.selectById(100L)).thenReturn(grade);

        ScheduleCourse course = new ScheduleCourse();
        course.setId(10L);
        course.setCourseName("HCIA");
        course.setTeacherCapacity(20);
        course.setStatus("ACTIVE");
        when(courseMapper.selectById(10L)).thenReturn(course);

        when(prerequisiteService.check(any(), any(), any())).thenReturn(SchedulingPrerequisiteResultVO.passed());
        UserContext.setUser(operator());

        SchedulingDispatchPlanServiceImpl service = new SchedulingDispatchPlanServiceImpl(
                progressMapper,
                gradeMapper,
                courseMapper,
                planMapper,
                planTeacherMapper,
                userMapper,
                prerequisiteService,
                auditLogService
        );
        return new TestContext(service, progress, progressMapper, planMapper, planTeacherMapper, userMapper, auditLogService);
    }

    private ScheduleDispatchPlanCreateDTO createDto(List<Long> teacherIds) {
        ScheduleDispatchPlanCreateDTO dto = new ScheduleDispatchPlanCreateDTO();
        dto.setCampusCourseProgressId(1000L);
        dto.setPlannedStartDate(LocalDate.of(2026, 7, 1));
        dto.setPlannedEndDate(LocalDate.of(2026, 7, 7));
        dto.setTeacherIds(teacherIds);
        dto.setRemark("暑期派课");
        return dto;
    }

    private ScheduleDispatchPlan plan(String status) {
        ScheduleDispatchPlan plan = new ScheduleDispatchPlan();
        plan.setId(900L);
        plan.setPlanNo("DP202607010001");
        plan.setCampusGradeId(100L);
        plan.setCourseId(10L);
        plan.setPlanStatus(status);
        plan.setExpectedStartDate(LocalDate.of(2026, 7, 1));
        plan.setExpectedEndDate(LocalDate.of(2026, 7, 7));
        plan.setIsDeleted((byte) 0);
        return plan;
    }

    private SysUser operator() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("管理员");
        user.setRole("ADMIN");
        return user;
    }

    private SysUser teacher(Long id, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(realName);
        return user;
    }

    private record TestContext(SchedulingDispatchPlanServiceImpl service,
                               ScheduleCampusCourseProgress progress,
                               ScheduleCampusCourseProgressMapper progressMapper,
                               ScheduleDispatchPlanMapper planMapper,
                               ScheduleDispatchPlanTeacherMapper planTeacherMapper,
                               SysUserMapper userMapper,
                               OperationAuditLogService auditLogService) {
    }
}
