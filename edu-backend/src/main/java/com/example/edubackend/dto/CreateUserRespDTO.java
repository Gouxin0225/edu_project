package com.example.edubackend.dto;

import lombok.Data;

@Data
public class CreateUserRespDTO {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String initialPassword;
}
