package com.example.edubackend.dto;

import lombok.Data;

@Data
public class AiResultActionConfirmVO {

    private String actionType;

    private String objectType;

    private Long objectId;

    private String message;
}
