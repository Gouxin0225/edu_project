package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ExamStatisticsVO {
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private BigDecimal passRate;
    private Integer totalStudents;
    private Integer passedStudents;
    private Map<String, Integer> scoreDistribution;
}
