package com.example.edubackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long id;
    private String realName;
    private String role;
    private Boolean mustChangePassword;
}
