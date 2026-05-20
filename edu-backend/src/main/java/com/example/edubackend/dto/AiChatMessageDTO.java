package com.example.edubackend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatMessageDTO {

    @Size(max = 50, message = "课程方向长度不能超过50个字符")
    private String courseCategory;

    @Size(max = 100, message = "知识点长度不能超过100个字符")
    private String knowledgePoint;

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000个字符")
    private String question;

    @Pattern(regexp = "^(SIMPLE|DETAILED|LESSON_PLAN|QUESTION)$", message = "回答模式不合法")
    private String mode = "DETAILED";

    @Pattern(regexp = "^(AUTO|PLATFORM_ONLY|KNOWLEDGE_ONLY|GENERAL_AI)$", message = "回答来源不合法")
    private String responseSource = "AUTO";

    @Pattern(regexp = "^(LEARN|PLATFORM_QUERY|KNOWLEDGE_QA|QUESTION_GEN|QUESTION_REVIEW|GENERAL)$", message = "任务意图不合法")
    private String intent;
}
