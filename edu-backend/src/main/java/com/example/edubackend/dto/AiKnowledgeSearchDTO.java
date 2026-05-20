package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeSearchDTO {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000个字符")
    private String question;

    @Size(max = 50, message = "课程方向长度不能超过50个字符")
    private String courseCategory;

    @Size(max = 100, message = "知识点长度不能超过100个字符")
    private String knowledgePoint;

    private Integer limit = 5;
}
