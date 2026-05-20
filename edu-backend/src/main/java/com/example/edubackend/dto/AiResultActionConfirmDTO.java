package com.example.edubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class AiResultActionConfirmDTO {

    @NotBlank(message = "转换动作不能为空")
    private String actionType;

    private Long sourceMessageId;

    @NotNull(message = "确认数据不能为空")
    private Map<String, Object> data;
}
