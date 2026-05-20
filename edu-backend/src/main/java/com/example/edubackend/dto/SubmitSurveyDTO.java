package com.example.edubackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitSurveyDTO {
    
    private Long surveyId;
    
    @Valid
    @NotEmpty(message = "答案不能为空")
    private List<AnswerItem> answers;
    
    @Data
    public static class AnswerItem {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;
        
        private String answerValue;
    }
}
