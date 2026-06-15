package com.example.edubackend.service.impl;

import com.example.edubackend.dto.MistakeVO;
import com.example.edubackend.dto.StudentDashboardVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.StudentMistakeBook;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.StudentMistakeBookMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentServiceImplTest {

    @Test
    void dashboardPutsSubmittedTasksInSubmittedGroupOnly() {
        StudentSubmission submission = new StudentSubmission();
        submission.setStatus("SUBMITTED");
        TestContext context = newContext(submission);

        StudentDashboardVO dashboard = context.service.getDashboard(9L);

        assertThat(dashboard.getSubmittedTasks())
                .extracting(StudentDashboardVO.TaskVO::getTaskId)
                .containsExactly(12L);
        assertThat(dashboard.getTodoTasks()).isEmpty();
        assertThat(dashboard.getPendingTasks()).isEmpty();
    }

    @Test
    void dashboardKeepsReturnedTasksActionable() {
        StudentSubmission submission = new StudentSubmission();
        submission.setStatus("RETURNED");
        TestContext context = newContext(submission);

        StudentDashboardVO dashboard = context.service.getDashboard(9L);

        assertThat(dashboard.getSubmittedTasks()).isEmpty();
        assertThat(dashboard.getTodoTasks())
                .extracting(StudentDashboardVO.TaskVO::getStatus)
                .containsExactly("RETURNED");
        assertThat(dashboard.getPendingTasks())
                .extracting(StudentDashboardVO.TaskVO::getStatus)
                .containsExactly("RETURNED");
    }

    @Test
    void mistakeListIncludesSoftDeletedQuestionSnapshots() {
        StudentMistakeBookMapper mistakeBookMapper = mock(StudentMistakeBookMapper.class);
        QuestionBankMapper questionBankMapper = mock(QuestionBankMapper.class);
        StudentServiceImpl service = newService(mistakeBookMapper, questionBankMapper);

        StudentMistakeBook mistake = new StudentMistakeBook();
        mistake.setId(6L);
        mistake.setStudentId(9L);
        mistake.setQuestionId(17L);
        mistake.setWrongCount(2);
        mistake.setIsMastered((byte) 0);

        QuestionBank deletedQuestion = new QuestionBank();
        deletedQuestion.setId(17L);
        deletedQuestion.setKnowledgePoint("Python函数");
        deletedQuestion.setType("SINGLE");
        deletedQuestion.setDifficulty("MEDIUM");
        deletedQuestion.setContent("以下哪个关键字用于定义函数？");
        deletedQuestion.setStandardAnswer("A");
        deletedQuestion.setIsDeleted((byte) 1);

        when(mistakeBookMapper.selectList(any())).thenReturn(List.of(mistake));
        when(questionBankMapper.selectBatchIdsIncludingDeleted(anyCollection())).thenReturn(List.of(deletedQuestion));

        List<MistakeVO> mistakes = service.getMistakeList(9L, null, null);

        assertThat(mistakes)
                .extracting(MistakeVO::getQuestionId)
                .containsExactly(17L);
        assertThat(mistakes.get(0).getContent()).isEqualTo("以下哪个关键字用于定义函数？");
        verify(questionBankMapper).selectBatchIdsIncludingDeleted(org.mockito.ArgumentMatchers.<Collection<Long>>argThat(ids ->
                ids.size() == 1 && ids.contains(17L)));
    }

    private TestContext newContext(StudentSubmission submission) {
        AssessmentTaskMapper assessmentTaskMapper = mock(AssessmentTaskMapper.class);
        StudentSubmissionMapper submissionMapper = mock(StudentSubmissionMapper.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        ClassStudentRelMapper classStudentRelMapper = mock(ClassStudentRelMapper.class);

        SysUser student = new SysUser();
        student.setId(9L);
        student.setClassId(3L);

        AssessmentTask task = new AssessmentTask();
        task.setId(12L);
        task.setType("HOMEWORK");
        task.setTitle("dashboard task");
        task.setEndTime(LocalDateTime.now().plusDays(5));

        when(sysUserMapper.selectById(9L)).thenReturn(student);
        when(classStudentRelMapper.selectList(any())).thenReturn(List.of());
        when(assessmentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(submissionMapper.selectOne(any())).thenReturn(submission);

        StudentServiceImpl service = newService(
                mock(StudentMistakeBookMapper.class),
                mock(QuestionBankMapper.class),
                assessmentTaskMapper,
                submissionMapper,
                sysUserMapper,
                classStudentRelMapper
        );
        return new TestContext(service);
    }

    private StudentServiceImpl newService(StudentMistakeBookMapper mistakeBookMapper, QuestionBankMapper questionBankMapper) {
        return newService(
                mistakeBookMapper,
                questionBankMapper,
                mock(AssessmentTaskMapper.class),
                mock(StudentSubmissionMapper.class),
                mock(SysUserMapper.class),
                mock(ClassStudentRelMapper.class)
        );
    }

    private StudentServiceImpl newService(
            StudentMistakeBookMapper mistakeBookMapper,
            QuestionBankMapper questionBankMapper,
            AssessmentTaskMapper assessmentTaskMapper,
            StudentSubmissionMapper submissionMapper,
            SysUserMapper sysUserMapper,
            ClassStudentRelMapper classStudentRelMapper) {
        return new StudentServiceImpl(
                assessmentTaskMapper,
                submissionMapper,
                mistakeBookMapper,
                questionBankMapper,
                sysUserMapper,
                classStudentRelMapper,
                new ObjectMapper(),
                mock(ExamSettingHelper.class)
        );
    }

    private record TestContext(StudentServiceImpl service) {
    }
}
