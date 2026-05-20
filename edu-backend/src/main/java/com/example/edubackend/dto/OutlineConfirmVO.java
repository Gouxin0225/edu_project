package com.example.edubackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class OutlineConfirmVO {
    private Integer savedCount;
    private List<Long> questionIds;
    private Long examId;
    private Long templateId;
}
