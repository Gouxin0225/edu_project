package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamStartVO {
    private Long submissionId;
    private List<ExamQuestionVO> questions;
    private Integer totalScore;
    private Integer duration;
    private Integer remainingSeconds;
    private Integer switchScreenLimit;
    private LocalDateTime deadline;
}
