package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamVO {
    private Long id;
    private String title;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalScore;
    private Integer duration;
    private Integer passScore;
    private Long creatorId;
    private List<Long> targetClassIds;
    private String status;
    private LocalDateTime createTime;
}
