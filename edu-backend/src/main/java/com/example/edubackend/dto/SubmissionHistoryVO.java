package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubmissionHistoryVO {
    private Long submissionId;
    private Integer version;
    private String status;
    private String content;
    private String gitLink;
    private String fileUrl;
    private BigDecimal scoreGained;
    private String teacherComment;
    private String assistantComment;
    private LocalDateTime submitTime;
}
