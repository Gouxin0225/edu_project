package com.example.edubackend.service.impl;

import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulingPrerequisiteServiceImplTest {

    @Test
    void hcipBlockedWhenHciaMissing() {
        TestContext context = newContext(course(20L, "HCIP", "HCIP"));
        when(context.courseMapper.selectList(any())).thenReturn(List.of(course(10L, "HCIA", "HCIA")));
        when(context.progressMapper.selectList(any())).thenReturn(List.of());

        SchedulingPrerequisiteResultVO result = context.service.check(1L, 100L, 20L);

        assertThat(result.getCanSchedule()).isFalse();
        assertThat(result.getBlockedReasons()).containsExactly("前置课程未完成: HCIA");
    }

    @Test
    void rhceBlockedWhenRhcsaMissing() {
        TestContext context = newContext(course(21L, "RHCE", "RHCE"));
        when(context.courseMapper.selectList(any())).thenReturn(List.of(course(11L, "RHCSA", "RHCSA")));
        when(context.progressMapper.selectList(any())).thenReturn(List.of(progress(100L, 11L, "IN_PROGRESS")));

        SchedulingPrerequisiteResultVO result = context.service.check(1L, 100L, 21L);

        assertThat(result.getCanSchedule()).isFalse();
        assertThat(result.getBlockedReasons()).containsExactly("前置课程未完成: RHCSA");
    }

    @Test
    void advancedCourseBlockedWhenMultipleCoursesMissing() {
        TestContext context = newContext(course(30L, "PENETRATION_TEST", "渗透测试"));
        ScheduleCourse hcia = course(10L, "HCIA", "HCIA");
        ScheduleCourse python = course(11L, "PYTHON", "Python");
        ScheduleCourse rhcsa = course(12L, "RHCSA", "RHCSA");
        ScheduleCourse web = course(13L, "WEB", "Web");
        ScheduleCourse mysql = course(14L, "MYSQL", "MySQL");
        ScheduleCourse hcip = course(15L, "HCIP", "HCIP");
        ScheduleCourse rhce = course(16L, "RHCE", "RHCE");
        when(context.courseMapper.selectList(any())).thenReturn(List.of(hcia, python, rhcsa, web, mysql, hcip, rhce));
        when(context.progressMapper.selectList(any())).thenReturn(List.of(
                progress(100L, hcia.getId(), "COMPLETED"),
                progress(100L, python.getId(), "PLANNED"),
                progress(100L, rhcsa.getId(), "COMPLETED"),
                progress(100L, web.getId(), "COMPLETED"),
                progress(100L, mysql.getId(), "COMPLETED")
        ));

        SchedulingPrerequisiteResultVO result = context.service.check(1L, 100L, 30L);

        assertThat(result.getCanSchedule()).isFalse();
        assertThat(result.getBlockedReasons()).containsExactly(
                "前置课程未完成: Python",
                "前置课程未完成: HCIP",
                "前置课程未完成: RHCE"
        );
    }

    @Test
    void basicCourseCanScheduleDirectly() {
        TestContext context = newContext(course(10L, "HCIA", "HCIA"));

        SchedulingPrerequisiteResultVO result = context.service.check(1L, 100L, 10L);

        assertThat(result.getCanSchedule()).isTrue();
        assertThat(result.getBlockedReasons()).isEmpty();
    }

    private TestContext newContext(ScheduleCourse targetCourse) {
        ScheduleCourseMapper courseMapper = mock(ScheduleCourseMapper.class);
        ScheduleCampusGradeMapper gradeMapper = mock(ScheduleCampusGradeMapper.class);
        ScheduleCampusCourseProgressMapper progressMapper = mock(ScheduleCampusCourseProgressMapper.class);

        ScheduleCampusGrade grade = new ScheduleCampusGrade();
        grade.setId(100L);
        grade.setSchoolName("北京校区");
        grade.setGradeName("2026春");

        when(gradeMapper.selectById(100L)).thenReturn(grade);
        when(courseMapper.selectById(targetCourse.getId())).thenReturn(targetCourse);

        SchedulingPrerequisiteServiceImpl service = new SchedulingPrerequisiteServiceImpl(
                courseMapper,
                gradeMapper,
                progressMapper
        );
        return new TestContext(service, courseMapper, progressMapper);
    }

    private ScheduleCourse course(Long id, String code, String name) {
        ScheduleCourse course = new ScheduleCourse();
        course.setId(id);
        course.setCourseCode(code);
        course.setCourseName(name);
        return course;
    }

    private ScheduleCampusCourseProgress progress(Long gradeId, Long courseId, String status) {
        ScheduleCampusCourseProgress progress = new ScheduleCampusCourseProgress();
        progress.setCampusGradeId(gradeId);
        progress.setCourseId(courseId);
        progress.setProgressStatus(status);
        return progress;
    }

    private record TestContext(SchedulingPrerequisiteServiceImpl service,
                               ScheduleCourseMapper courseMapper,
                               ScheduleCampusCourseProgressMapper progressMapper) {
    }
}
