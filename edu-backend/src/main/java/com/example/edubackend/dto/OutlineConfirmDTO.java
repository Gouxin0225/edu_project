package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutlineConfirmDTO {
    private Boolean createExam = false;
    private Boolean createTemplate = false;
    private String examTitle;
    private String templateName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Long> targetClassIds;
}
