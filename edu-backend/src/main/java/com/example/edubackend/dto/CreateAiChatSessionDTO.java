package com.example.edubackend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAiChatSessionDTO {

    @Size(max = 100, message = "会话标题长度不能超过100个字符")
    private String title;

    @Size(max = 50, message = "课程方向长度不能超过50个字符")
    private String courseCategory;

    @Size(max = 100, message = "知识点长度不能超过100个字符")
    private String knowledgePoint;
}
