package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiResultActionPreviewDTO {

    @NotBlank(message = "转换动作不能为空")
    private String actionType;

    private Long sourceMessageId;

    private String sourceContent;

    private String courseCategory;

    private String knowledgePoint;
}
