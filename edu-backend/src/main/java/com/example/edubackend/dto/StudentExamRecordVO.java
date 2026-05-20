package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentExamRecordVO {
    private Long examId;
    private String title;
    private LocalDateTime deadline;
    private LocalDateTime submitTime;
    private String status;
    private BigDecimal scoreGained;
}
