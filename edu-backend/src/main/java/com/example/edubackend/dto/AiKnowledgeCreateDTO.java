package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeCreateDTO {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "课程方向不能为空")
    @Size(max = 50, message = "课程方向长度不能超过50个字符")
    private String courseCategory;

    @Size(max = 100, message = "知识点长度不能超过100个字符")
    private String knowledgePoint;

    @NotBlank(message = "文档内容不能为空")
    private String content;

    @Pattern(regexp = "^(PRIVATE|PUBLIC)$", message = "可见范围不合法")
    private String visibility = "PRIVATE";

    @Pattern(regexp = "^(STUDENT_SAFE|TEACHER_ONLY)$", message = "引用范围不合法")
    private String accessScope = "TEACHER_ONLY";
}
