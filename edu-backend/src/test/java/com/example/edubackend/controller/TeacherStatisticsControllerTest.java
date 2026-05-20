package com.example.edubackend.controller;

import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.mapper.AiChatMessageMapper;
import com.example.edubackend.mapper.AiKnowledgeDocumentMapper;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.StudentAnswerDetailMapper;
import com.example.edubackend.mapper.StudentReviewNoteMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TaskQuestionRelMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TeacherStatisticsControllerTest {

    @Test
    void buildQuestionScoreMapSkipsQueryWhenTaskIdsAreEmpty() {
        TaskQuestionRelMapper taskQuestionRelMapper = mock(TaskQuestionRelMapper.class);
        TeacherStatisticsController controller = newController(taskQuestionRelMapper);

        AssessmentTask taskWithoutId = new AssessmentTask();
        Map<String, Integer> result = invokeBuildQuestionScoreMap(controller, List.of(taskWithoutId));

        assertThat(result).isEmpty();
        verifyNoInteractions(taskQuestionRelMapper);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> invokeBuildQuestionScoreMap(
            TeacherStatisticsController controller,
            List<AssessmentTask> tasks) {
        return (Map<String, Integer>) ReflectionTestUtils.invokeMethod(controller, "buildQuestionScoreMap", tasks);
    }

    private TeacherStatisticsController newController(TaskQuestionRelMapper taskQuestionRelMapper) {
        return new TeacherStatisticsController(
                mock(TeacherClassRelMapper.class),
                mock(SysClassMapper.class),
                mock(SysUserMapper.class),
                mock(AssessmentTaskMapper.class),
                mock(StudentSubmissionMapper.class),
                mock(SurveyTaskMapper.class),
                mock(SurveyRecordMapper.class),
                mock(StudentAnswerDetailMapper.class),
                taskQuestionRelMapper,
                mock(QuestionBankMapper.class),
                mock(AiChatMessageMapper.class),
                mock(AiKnowledgeDocumentMapper.class),
                mock(StudentReviewNoteMapper.class),
                new ObjectMapper(),
                mock(ExamSettingHelper.class)
        );
    }
}
