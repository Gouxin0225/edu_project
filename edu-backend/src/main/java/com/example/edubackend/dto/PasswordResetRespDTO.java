package com.example.edubackend.dto;

import lombok.Data;

@Data
public class PasswordResetRespDTO {
    private Long userId;
    private String username;
    private String newPassword;
    private String message;
}
