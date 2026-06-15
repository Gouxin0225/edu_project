package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HomeworkListVO {
    private Long homeworkId;
    private String title;
    private String content;
    private LocalDateTime deadline;
    private Integer totalSubmissions;
    private Integer targetStudentCount;
    private Integer gradedSubmissions;
    private Integer pendingSubmissions;
}
