package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentHomeworkVO {
    private Long homeworkId;
    private Long submissionId;
    private String title;
    private String content;
    private LocalDateTime deadline;
    private String status;
    private BigDecimal scoreGained;
    private LocalDateTime submitTime;
    private Integer version;
}
