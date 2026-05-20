package com.example.edubackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiChatContextResult {
    private String context;
    private String intent;
    private String responseSource;
    private String sourceSummary;
}
