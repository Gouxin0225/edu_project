package com.example.edubackend.service.impl;

import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleAdvancedBatchDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchVO;
import com.example.edubackend.entity.ScheduleAdvancedBatch;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleAdvancedBatchMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.ScheduleDispatchPlanMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.OperationAuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleAdvancedBatchServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void createBatchAcceptsOnlyConfiguredAdvancedCourses() {
        TestContext context = newContext(course(10L, "PENETRATION_TEST", "渗透测试"));
        doAnswer(invocation -> {
            ScheduleAdvancedBatch batch = invocation.getArgument(0);
            batch.setId(700L);
            return 1;
        }).when(context.batchMapper).insert(any(ScheduleAdvancedBatch.class));

        ScheduleAdvancedBatchVO result = context.service.createBatch(dto(10L));

        assertThat(result.getId()).isEqualTo(700L);
        assertThat(result.getCourseName()).isEqualTo("渗透测试");
        assertThat(result.getBatchStatus()).isEqualTo("DRAFT");
        assertThat(result.getCreatedBy()).isEqualTo(1L);
        verify(context.auditLogService).record(eq("CREATE"), eq("SCHEDULE_ADVANCED_BATCH"), eq(700L), any());
    }

    @Test
    void createBatchRejectsNonAdvancedCourse() {
        TestContext context = newContext(course(20L, "HCIA", "HCIA"));

        assertThatThrownBy(() -> context.service.createBatch(dto(20L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只允许创建");
    }

    private TestContext newContext(ScheduleCourse course) {
        ScheduleAdvancedBatchMapper batchMapper = mock(ScheduleAdvancedBatchMapper.class);
        ScheduleCourseMapper courseMapper = mock(ScheduleCourseMapper.class);
        ScheduleDispatchPlanMapper dispatchPlanMapper = mock(ScheduleDispatchPlanMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        OperationAuditLogService auditLogService = mock(OperationAuditLogService.class);
        when(courseMapper.selectById(course.getId())).thenReturn(course);
        UserContext.setUser(operator());
        ScheduleAdvancedBatchServiceImpl service = new ScheduleAdvancedBatchServiceImpl(
                batchMapper,
                courseMapper,
                dispatchPlanMapper,
                userMapper,
                auditLogService
        );
        return new TestContext(service, batchMapper, auditLogService);
    }

    private ScheduleAdvancedBatchDTO dto(Long courseId) {
        ScheduleAdvancedBatchDTO dto = new ScheduleAdvancedBatchDTO();
        dto.setBatchName("高级课一期");
        dto.setCourseId(courseId);
        dto.setSchoolName("中心");
        dto.setExpectedStartDate(LocalDate.of(2026, 8, 1));
        dto.setExpectedEndDate(LocalDate.of(2026, 8, 10));
        dto.setCapacity(30);
        return dto;
    }

    private ScheduleCourse course(Long id, String code, String name) {
        ScheduleCourse course = new ScheduleCourse();
        course.setId(id);
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setStatus("ACTIVE");
        course.setIsDeleted((byte) 0);
        return course;
    }

    private SysUser operator() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("管理员");
        user.setRole("ADMIN");
        return user;
    }

    private record TestContext(ScheduleAdvancedBatchServiceImpl service,
                               ScheduleAdvancedBatchMapper batchMapper,
                               OperationAuditLogService auditLogService) {
    }
}
