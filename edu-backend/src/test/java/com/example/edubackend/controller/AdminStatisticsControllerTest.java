package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.edubackend.dto.AdminClassStatisticsVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SurveyAnswerDetail;
import com.example.edubackend.entity.SurveyQuestion;
import com.example.edubackend.entity.SurveyRecord;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SurveyAnswerDetailMapper;
import com.example.edubackend.mapper.SurveyQuestionMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminStatisticsControllerTest {

    static {
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(builderAssistant, SysClass.class);
        TableInfoHelper.initTableInfo(builderAssistant, AssessmentTask.class);
        TableInfoHelper.initTableInfo(builderAssistant, SurveyTask.class);
    }

    private final SysClassMapper sysClassMapper = mock(SysClassMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final TeacherClassRelMapper teacherClassRelMapper = mock(TeacherClassRelMapper.class);
    private final AssessmentTaskMapper assessmentTaskMapper = mock(AssessmentTaskMapper.class);
    private final StudentSubmissionMapper studentSubmissionMapper = mock(StudentSubmissionMapper.class);
    private final SurveyTaskMapper surveyTaskMapper = mock(SurveyTaskMapper.class);
    private final SurveyRecordMapper surveyRecordMapper = mock(SurveyRecordMapper.class);
    private final SurveyQuestionMapper surveyQuestionMapper = mock(SurveyQuestionMapper.class);
    private final SurveyAnswerDetailMapper surveyAnswerDetailMapper = mock(SurveyAnswerDetailMapper.class);

    private final AdminStatisticsController controller = new AdminStatisticsController(
            sysClassMapper,
            sysUserMapper,
            teacherClassRelMapper,
            assessmentTaskMapper,
            studentSubmissionMapper,
            surveyTaskMapper,
            surveyRecordMapper,
            surveyQuestionMapper,
            surveyAnswerDetailMapper,
            new ObjectMapper()
    );

    @Test
    void classOverviewCountsDistinctTasksWhenOneTaskTargetsMultipleClasses() {
        when(sysClassMapper.selectList(any())).thenReturn(List.of(
                clazz(1L, "一班"),
                clazz(2L, "二班")
        ));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(
                student(11L, 1L),
                student(12L, 2L)
        ));
        when(teacherClassRelMapper.selectList(any())).thenReturn(List.<TeacherClassRel>of());
        when(assessmentTaskMapper.selectList(any())).thenReturn(List.of(
                task(101L, "EXAM", "[1,2]"),
                task(102L, "EXAM", "[1]"),
                task(201L, "HOMEWORK", "[1,2]")
        ));
        when(studentSubmissionMapper.selectList(any())).thenReturn(List.<StudentSubmission>of());
        when(surveyTaskMapper.selectList(any())).thenReturn(List.of(
                survey(301L, "[1,2]")
        ));
        when(surveyRecordMapper.selectList(any())).thenReturn(List.<SurveyRecord>of());
        when(surveyQuestionMapper.selectList(any())).thenReturn(List.<SurveyQuestion>of());
        when(surveyAnswerDetailMapper.selectList(any())).thenReturn(List.<SurveyAnswerDetail>of());

        Result<AdminClassStatisticsVO> result = controller.getClassStatistics();

        AdminClassStatisticsVO data = result.getData();
        assertThat(data.getOverview().getExamCount()).isEqualTo(2);
        assertThat(data.getOverview().getHomeworkCount()).isEqualTo(1);
        assertThat(data.getOverview().getSurveyCount()).isEqualTo(1);
        assertThat(data.getClasses())
                .extracting(clazz -> clazz.getExamSummary().getTaskCount())
                .containsExactly(2, 1);
        assertActiveScopeWasUsed();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertActiveScopeWasUsed() {
        ArgumentCaptor<LambdaQueryWrapper<SysClass>> classQueryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        ArgumentCaptor<LambdaQueryWrapper<AssessmentTask>> taskQueryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        ArgumentCaptor<LambdaQueryWrapper<SurveyTask>> surveyQueryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);

        verify(sysClassMapper).selectList(classQueryCaptor.capture());
        verify(assessmentTaskMapper).selectList(taskQueryCaptor.capture());
        verify(surveyTaskMapper).selectList(surveyQueryCaptor.capture());

        assertThat(classQueryCaptor.getValue().getSqlSegment()).contains("is_deleted");
        assertThat(taskQueryCaptor.getValue().getSqlSegment()).contains("is_deleted");
        assertThat(surveyQueryCaptor.getValue().getSqlSegment()).contains("is_deleted");
    }

    private SysClass clazz(Long id, String name) {
        SysClass clazz = new SysClass();
        clazz.setId(id);
        clazz.setClassName(name);
        clazz.setStatus((byte) 1);
        return clazz;
    }

    private SysUser student(Long id, Long classId) {
        SysUser student = new SysUser();
        student.setId(id);
        student.setUsername("student" + id);
        student.setRealName("学生" + id);
        student.setRole("STUDENT");
        student.setClassId(classId);
        student.setIsDeleted((byte) 0);
        return student;
    }

    private AssessmentTask task(Long id, String type, String targetClassIds) {
        AssessmentTask task = new AssessmentTask();
        task.setId(id);
        task.setTitle(type + id);
        task.setType(type);
        task.setTargetClassIds(targetClassIds);
        return task;
    }

    private SurveyTask survey(Long id, String targetClassIds) {
        SurveyTask survey = new SurveyTask();
        survey.setId(id);
        survey.setTitle("survey" + id);
        survey.setTargetClassIds(targetClassIds);
        survey.setStatus((byte) 1);
        return survey;
    }
}
