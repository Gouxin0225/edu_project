package com.example.edubackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SaveAnswerDTO {
    
    @NotNull(message = "答案列表不能为空")
    private List<AnswerItem> answers;
    
    @Data
    public static class AnswerItem {
        private Long questionId;
        private String answerValue;
    }
}
