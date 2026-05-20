package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AIGradeSuggestVO {
    private BigDecimal suggestedScore;
    private BigDecimal maxScore;
    private String reasoning;
    private String suggestion;
    private List<String> keyPoints = new ArrayList<>();
    private List<String> matchedPoints = new ArrayList<>();
    private List<String> missingPoints = new ArrayList<>();
    private String confidence;
}
