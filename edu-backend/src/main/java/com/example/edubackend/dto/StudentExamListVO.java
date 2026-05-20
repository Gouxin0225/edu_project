package com.example.edubackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentExamListVO {
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalScore;
    private String status;
    private Long submissionId;
    private Integer scoreGained;
}
