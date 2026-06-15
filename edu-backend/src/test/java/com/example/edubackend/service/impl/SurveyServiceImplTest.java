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
    void createSurveyWithoutClientQuestionsWritesFixedQuestions() {
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

        service.createSurvey(dto, 1L);

        ArgumentCaptor<SurveyQuestion> captor = ArgumentCaptor.forClass(SurveyQuestion.class);
        verify(surveyQuestionMapper, org.mockito.Mockito.times(6)).insert(captor.capture());
        List<SurveyQuestion> questions = captor.getAllValues();

        assertThat(questions).extracting(SurveyQuestion::getTitle).containsExactly(
                "您对这次授课讲师打几分？满分5分",
                "您对讲师授课的逻辑性打几分？满分5分",
                "您对讲师上课的趣味性打几分？满分5分",
                "讲师对学员关系度打几分？满分5分",
                "讲师理论与工程结合能力您打几分？满分5分",
                "您对授课讲师的建议"
        );
        assertThat(questions.subList(0, 5)).allSatisfy(fixedQuestion -> {
            assertThat(fixedQuestion.getType()).isEqualTo("STAR");
            assertThat(fixedQuestion.getOptionsJson()).isEqualTo("[1,2,3,4,5]");
            assertThat(fixedQuestion.getIsRequired()).isEqualTo((byte) 1);
        });
        assertThat(questions.get(5).getType()).isEqualTo("TEXT");
        assertThat(questions.get(5).getOptionsJson()).isEqualTo("[]");
    }

    @Test
    void createSurveyRejectsCustomQuestions() {
        SurveyTaskMapper surveyTaskMapper = mock(SurveyTaskMapper.class);
        SurveyQuestionMapper surveyQuestionMapper = mock(SurveyQuestionMapper.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        SurveyServiceImpl service = newService(surveyTaskMapper, surveyQuestionMapper, sysUserMapper);

        CreateSurveyDTO dto = new CreateSurveyDTO();
        dto.setTitle("自定义问卷");
        dto.setEndTime(LocalDateTime.now().plusDays(1));
        CreateSurveyDTO.QuestionConfig question = new CreateSurveyDTO.QuestionConfig();
        question.setType("TEXT");
        question.setTitle("客户端传入的自定义题目");
        dto.setQuestions(List.of(question));

        assertThatThrownBy(() -> service.createSurvey(dto, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("固定问卷题目");
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
