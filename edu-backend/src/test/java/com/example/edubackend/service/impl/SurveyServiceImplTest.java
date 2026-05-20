package com.example.edubackend.service.impl;

import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateSurveyDTO;
import com.example.edubackend.entity.SurveyQuestion;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.SurveyAnswerDetailMapper;
import com.example.edubackend.mapper.SurveyQuestionMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SurveyServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    void createSurveyWritesDefaultScaleOptionsAsJsonArray() throws Exception {
        SurveyTaskMapper surveyTaskMapper = mock(SurveyTaskMapper.class);
        SurveyQuestionMapper surveyQuestionMapper = mock(SurveyQuestionMapper.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        SurveyServiceImpl service = newService(surveyTaskMapper, surveyQuestionMapper, sysUserMapper);
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole("ADMIN");
        UserContext.setUser(admin);
        when(sysUserMapper.selectById(1L)).thenReturn(admin);
        when(surveyTaskMapper.insert(any(SurveyTask.class))).thenReturn(1);

        CreateSurveyDTO dto = new CreateSurveyDTO();
        dto.setTitle("满意度问卷");
        dto.setEndTime(LocalDateTime.now().plusDays(1));
        CreateSurveyDTO.QuestionConfig question = new CreateSurveyDTO.QuestionConfig();
        question.setType("SCALE");
        question.setTitle("课程体验");
        question.setOptionsJson(" ");
        dto.setQuestions(List.of(question));

        service.createSurvey(dto, 1L);

        ArgumentCaptor<SurveyQuestion> captor = ArgumentCaptor.forClass(SurveyQuestion.class);
        verify(surveyQuestionMapper).insert(captor.capture());
        List<String> options = objectMapper.readValue(
                captor.getValue().getOptionsJson(),
                new TypeReference<>() {});
        assertThat(options).containsExactly("非常不同意", "不同意", "一般", "同意", "非常同意");
    }

    @Test
    void normalizeQuestionOptionsRejectsInvalidJson() {
        SurveyServiceImpl service = newService(mock(SurveyTaskMapper.class), mock(SurveyQuestionMapper.class), mock(SysUserMapper.class));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service, "normalizeQuestionOptions", "SINGLE", "{bad json"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("合法JSON");
    }

    private SurveyServiceImpl newService(
            SurveyTaskMapper surveyTaskMapper,
            SurveyQuestionMapper surveyQuestionMapper,
            SysUserMapper sysUserMapper) {
        return new SurveyServiceImpl(
                surveyTaskMapper,
                surveyQuestionMapper,
                mock(SurveyRecordMapper.class),
                mock(SurveyAnswerDetailMapper.class),
                sysUserMapper,
                mock(TeacherClassRelMapper.class),
                mock(ClassStudentRelMapper.class),
                objectMapper
        );
    }
}
