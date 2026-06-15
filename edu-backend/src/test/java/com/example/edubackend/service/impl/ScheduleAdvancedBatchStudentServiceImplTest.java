package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentImportDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentVO;
import com.example.edubackend.entity.ScheduleAdvancedBatch;
import com.example.edubackend.entity.ScheduleAdvancedBatchStudent;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.ScheduleAdvancedBatchMapper;
import com.example.edubackend.mapper.ScheduleAdvancedBatchStudentMapper;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.OperationAuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleAdvancedBatchStudentServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void importStudentsMarksEligibleWhenAllRequiredGradeCoursesCompleted() {
        TestContext context = newContext();
        when(context.progressMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(progress("COMPLETED"));

        List<ScheduleAdvancedBatchStudentVO> result = context.service.importStudents(700L, importDto(1000L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEligible()).isTrue();
        assertThat(result.get(0).getEligibilityStatus()).isEqualTo("ELIGIBLE");
        assertThat(result.get(0).getMissingCourses()).isEmpty();
        assertThat(context.saved.get(0).getCampusGradeId()).isEqualTo(300L);
        verify(context.auditLogService).record(eq("IMPORT"), eq("SCHEDULE_ADVANCED_BATCH_STUDENT"), eq(700L), any());
    }

    @Test
    void importStudentsReturnsMissingCourseListWhenGradeCoursesNotCompleted() {
        TestContext context = newContext();
        when(context.progressMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(progress("COMPLETED"))
                .thenReturn(progress("COMPLETED"))
                .thenReturn(progress("COMPLETED"))
                .thenReturn(progress("COMPLETED"))
                .thenReturn(progress("COMPLETED"))
                .thenReturn(progress("NOT_STARTED"))
                .thenReturn(null);

        List<ScheduleAdvancedBatchStudentVO> result = context.service.importStudents(700L, importDto(1000L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEligible()).isFalse();
        assertThat(result.get(0).getEligibilityStatus()).isEqualTo("INELIGIBLE");
        assertThat(result.get(0).getMissingCourses()).containsExactly("HCIP", "RHCE");
        assertThat(context.saved.get(0).getIneligibleReason()).isEqualTo("HCIP、RHCE");
    }

    private TestContext newContext() {
        ScheduleAdvancedBatchMapper batchMapper = mock(ScheduleAdvancedBatchMapper.class);
        ScheduleAdvancedBatchStudentMapper batchStudentMapper = mock(ScheduleAdvancedBatchStudentMapper.class);
        ScheduleCampusGradeMapper campusGradeMapper = mock(ScheduleCampusGradeMapper.class);
        ScheduleCampusCourseProgressMapper progressMapper = mock(ScheduleCampusCourseProgressMapper.class);
        ScheduleCourseMapper courseMapper = mock(ScheduleCourseMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysClassMapper classMapper = mock(SysClassMapper.class);
        ClassStudentRelMapper classStudentRelMapper = mock(ClassStudentRelMapper.class);
        OperationAuditLogService auditLogService = mock(OperationAuditLogService.class);

        ScheduleAdvancedBatch batch = new ScheduleAdvancedBatch();
        batch.setId(700L);
        batch.setBatchName("高级安全课一期");
        batch.setIsDeleted((byte) 0);
        when(batchMapper.selectById(700L)).thenReturn(batch);
        when(batchStudentMapper.selectOne(any())).thenReturn(null);
        List<ScheduleAdvancedBatchStudent> saved = new ArrayList<>();
        doAnswer(invocation -> {
            ScheduleAdvancedBatchStudent record = invocation.getArgument(0);
            record.setId(1L);
            saved.add(record);
            return 1;
        }).when(batchStudentMapper).insert(any(ScheduleAdvancedBatchStudent.class));

        SysUser student = student(1000L);
        when(userMapper.selectList(any())).thenReturn(List.of(student));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(student));

        SysClass sysClass = new SysClass();
        sysClass.setId(200L);
        sysClass.setClassName("北京 2026 春");
        sysClass.setSchoolName("北京校区");
        when(classMapper.selectById(200L)).thenReturn(sysClass);
        when(classMapper.selectBatchIds(any())).thenReturn(List.of(sysClass));

        ScheduleCampusGrade grade = new ScheduleCampusGrade();
        grade.setId(300L);
        grade.setClassId(200L);
        grade.setSchoolName("北京校区");
        grade.setGradeName("2026春");
        when(campusGradeMapper.selectOne(any())).thenReturn(grade);
        when(campusGradeMapper.selectBatchIds(any())).thenReturn(List.of(grade));
        when(courseMapper.selectList(any())).thenReturn(requiredCourses());

        UserContext.setUser(operator());
        ScheduleAdvancedBatchStudentServiceImpl service = new ScheduleAdvancedBatchStudentServiceImpl(
                batchMapper,
                batchStudentMapper,
                campusGradeMapper,
                progressMapper,
                courseMapper,
                userMapper,
                classMapper,
                classStudentRelMapper,
                auditLogService
        );
        return new TestContext(service, progressMapper, auditLogService, saved);
    }

    private ScheduleAdvancedBatchStudentImportDTO importDto(Long studentId) {
        ScheduleAdvancedBatchStudentImportDTO dto = new ScheduleAdvancedBatchStudentImportDTO();
        dto.setStudentIds(List.of(studentId));
        return dto;
    }

    private SysUser student(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("stu" + id);
        user.setRealName("学生" + id);
        user.setRole("STUDENT");
        user.setClassId(200L);
        user.setIsDeleted((byte) 0);
        return user;
    }

    private SysUser operator() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("管理员");
        user.setRole("ADMIN");
        return user;
    }

    private List<ScheduleCourse> requiredCourses() {
        return List.of(
                course(1L, "HCIA"),
                course(2L, "PYTHON"),
                course(3L, "RHCSA"),
                course(4L, "WEB"),
                course(5L, "MYSQL"),
                course(6L, "HCIP"),
                course(7L, "RHCE")
        );
    }

    private ScheduleCourse course(Long id, String code) {
        ScheduleCourse course = new ScheduleCourse();
        course.setId(id);
        course.setCourseCode(code);
        course.setCourseName(code);
        course.setStatus("ACTIVE");
        course.setIsDeleted((byte) 0);
        return course;
    }

    private ScheduleCampusCourseProgress progress(String status) {
        ScheduleCampusCourseProgress progress = new ScheduleCampusCourseProgress();
        progress.setCampusGradeId(300L);
        progress.setProgressStatus(status);
        return progress;
    }

    private record TestContext(ScheduleAdvancedBatchStudentServiceImpl service,
                               ScheduleCampusCourseProgressMapper progressMapper,
                               OperationAuditLogService auditLogService,
                               List<ScheduleAdvancedBatchStudent> saved) {
    }
}
