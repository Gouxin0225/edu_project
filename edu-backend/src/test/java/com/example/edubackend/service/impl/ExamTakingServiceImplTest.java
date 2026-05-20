package com.example.edubackend.service.impl;

import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.IQuestionBankService;
import com.example.edubackend.service.IStudentAnswerDetailService;
import com.example.edubackend.service.IStudentMistakeBookService;
import com.example.edubackend.service.IStudentSubmissionService;
import com.example.edubackend.service.ITaskQuestionRelService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamTakingServiceImplTest {

    @Test
    void loadQuestionMapSkipsQueryWhenQuestionIdsAreEmpty() {
        IQuestionBankService questionBankService = mock(IQuestionBankService.class);
        ExamTakingServiceImpl service = newService(questionBankService);

        TaskQuestionRel rel = new TaskQuestionRel();
        rel.setQuestionId(null);

        Map<Long, QuestionBank> result = invokeLoadQuestionMap(service, List.of(rel));

        assertThat(result).isEmpty();
        verify(questionBankService, never()).listByIds(anyCollection());
    }

    @Test
    void loadQuestionMapDeduplicatesQuestionIdsBeforeBatchQuery() {
        IQuestionBankService questionBankService = mock(IQuestionBankService.class);
        ExamTakingServiceImpl service = newService(questionBankService);
        QuestionBank question = new QuestionBank();
        question.setId(7L);
        when(questionBankService.listByIds(anyCollection())).thenReturn(List.of(question));

        TaskQuestionRel first = new TaskQuestionRel();
        first.setQuestionId(7L);
        TaskQuestionRel second = new TaskQuestionRel();
        second.setQuestionId(7L);

        Map<Long, QuestionBank> result = invokeLoadQuestionMap(service, List.of(first, second));

        assertThat(result).containsEntry(7L, question);
        verify(questionBankService).listByIds(org.mockito.ArgumentMatchers.<Collection<Long>>argThat(ids ->
                ids.size() == 1 && ids.contains(7L)));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, QuestionBank> invokeLoadQuestionMap(ExamTakingServiceImpl service, List<TaskQuestionRel> rels) {
        return (Map<Long, QuestionBank>) ReflectionTestUtils.invokeMethod(service, "loadQuestionMap", rels);
    }

    private ExamTakingServiceImpl newService(IQuestionBankService questionBankService) {
        return new ExamTakingServiceImpl(
                mock(IAssessmentTaskService.class),
                mock(IStudentSubmissionService.class),
                mock(IStudentAnswerDetailService.class),
                mock(IStudentMistakeBookService.class),
                questionBankService,
                mock(ITaskQuestionRelService.class),
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                mock(SysUserMapper.class),
                mock(ClassStudentRelMapper.class),
                mock(ExamSettingHelper.class)
        );
    }
}
