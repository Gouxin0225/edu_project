package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AssistantExamVO {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalScore;
    private String status;
    private List<String> targetClassNames = new ArrayList<>();
    private Integer targetStudentCount = 0;
    private Integer submittedCount = 0;
    private Integer inProgressCount = 0;
    private Integer notSubmittedCount = 0;
    private BigDecimal averageScore = BigDecimal.ZERO;
    private BigDecimal passRate = BigDecimal.ZERO;
}
