package com.example.edubackend.service.impl;

import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.IStudentAnswerDetailService;
import com.example.edubackend.service.IStudentMistakeBookService;
import com.example.edubackend.service.IStudentSubmissionService;
import com.example.edubackend.service.ITaskQuestionRelService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamTakingServiceImplTest {

    @Test
    void loadQuestionMapSkipsQueryWhenQuestionIdsAreEmpty() {
        QuestionBankMapper questionBankMapper = mock(QuestionBankMapper.class);
        ExamTakingServiceImpl service = newService(questionBankMapper);

        TaskQuestionRel rel = new TaskQuestionRel();
        rel.setQuestionId(null);

        Map<Long, QuestionBank> result = invokeLoadQuestionMap(service, List.of(rel));

        assertThat(result).isEmpty();
        verify(questionBankMapper, never()).selectBatchIdsIncludingDeleted(anyCollection());
    }

    @Test
    void loadQuestionMapDeduplicatesQuestionIdsBeforeBatchQuery() {
        QuestionBankMapper questionBankMapper = mock(QuestionBankMapper.class);
        ExamTakingServiceImpl service = newService(questionBankMapper);
        QuestionBank question = new QuestionBank();
        question.setId(7L);
        when(questionBankMapper.selectBatchIdsIncludingDeleted(anyCollection())).thenReturn(List.of(question));

        TaskQuestionRel first = new TaskQuestionRel();
        first.setQuestionId(7L);
        TaskQuestionRel second = new TaskQuestionRel();
        second.setQuestionId(7L);

        Map<Long, QuestionBank> result = invokeLoadQuestionMap(service, List.of(first, second));

        assertThat(result).containsEntry(7L, question);
        verify(questionBankMapper).selectBatchIdsIncludingDeleted(org.mockito.ArgumentMatchers.<Collection<Long>>argThat(ids ->
                ids.size() == 1 && ids.contains(7L)));
    }

    @Test
    void expiredRequiredSurveyStillBlocksExamSubmitWhenUnsubmitted() {
        SurveyTaskMapper surveyTaskMapper = mock(SurveyTaskMapper.class);
        SurveyRecordMapper surveyRecordMapper = mock(SurveyRecordMapper.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        ClassStudentRelMapper classStudentRelMapper = mock(ClassStudentRelMapper.class);
        ExamSettingHelper examSettingHelper = mock(ExamSettingHelper.class);
        ExamTakingServiceImpl service = newService(
                mock(QuestionBankMapper.class),
                sysUserMapper,
                classStudentRelMapper,
                surveyTaskMapper,
                surveyRecordMapper,
                examSettingHelper
        );

        AssessmentTask exam = new AssessmentTask();
        exam.setId(38L);
        SurveyTask survey = new SurveyTask();
        survey.setId(21L);
        survey.setStatus((byte) 1);
        survey.setEndTime(LocalDateTime.now().minusDays(1));
        survey.setTargetClassIds("[5]");
        SysUser student = new SysUser();
        student.setId(44L);
        student.setClassId(5L);

        when(examSettingHelper.isSurveyRequiredBeforeSubmit(exam)).thenReturn(true);
        when(examSettingHelper.getRequiredSurveyId(exam)).thenReturn(21L);
        when(surveyTaskMapper.selectById(21L)).thenReturn(survey);
        when(sysUserMapper.selectById(44L)).thenReturn(student);
        when(classStudentRelMapper.selectList(any())).thenReturn(List.of());
        when(surveyRecordMapper.selectCount(any())).thenReturn(0L);

        Boolean pending = ReflectionTestUtils.invokeMethod(service, "isRequiredSurveyPending", exam, 44L);

        assertThat(pending).isTrue();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, QuestionBank> invokeLoadQuestionMap(ExamTakingServiceImpl service, List<TaskQuestionRel> rels) {
        return (Map<Long, QuestionBank>) ReflectionTestUtils.invokeMethod(service, "loadQuestionMap", rels);
    }

    private ExamTakingServiceImpl newService(QuestionBankMapper questionBankMapper) {
        return newService(
                questionBankMapper,
                mock(SysUserMapper.class),
                mock(ClassStudentRelMapper.class),
                mock(SurveyTaskMapper.class),
                mock(SurveyRecordMapper.class),
                mock(ExamSettingHelper.class)
        );
    }

    private ExamTakingServiceImpl newService(
            QuestionBankMapper questionBankMapper,
            SysUserMapper sysUserMapper,
            ClassStudentRelMapper classStudentRelMapper,
            SurveyTaskMapper surveyTaskMapper,
            SurveyRecordMapper surveyRecordMapper,
            ExamSettingHelper examSettingHelper) {
        return new ExamTakingServiceImpl(
                mock(IAssessmentTaskService.class),
                mock(IStudentSubmissionService.class),
                mock(IStudentAnswerDetailService.class),
                mock(IStudentMistakeBookService.class),
                mock(ITaskQuestionRelService.class),
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                questionBankMapper,
                sysUserMapper,
                classStudentRelMapper,
                surveyTaskMapper,
                surveyRecordMapper,
                examSettingHelper,
                mock(TransactionTemplate.class)
        );
    }
}
