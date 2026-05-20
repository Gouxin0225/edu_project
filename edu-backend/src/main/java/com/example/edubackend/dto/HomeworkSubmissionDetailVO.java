package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HomeworkSubmissionDetailVO {
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private String status;
    private BigDecimal totalScoreGained;
    private LocalDateTime submitTime;
    private String content;
    private String gitLink;
    private String fileUrl;
    private String teacherComment;
    private String assistantComment;
}
