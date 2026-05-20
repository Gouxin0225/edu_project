package com.example.edubackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AiResultActionPreviewVO {

    private String actionType;

    private Long sourceMessageId;

    private String objectType;

    private Map<String, Object> data;

    private List<String> warnings = new ArrayList<>();
}
