package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubmissionListVO {
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private String status;
    private BigDecimal totalScoreGained;
    private BigDecimal scoreGained;
    private LocalDateTime submitTime;
    private Integer switchScreenCount;
    private String assistantComment;
}
