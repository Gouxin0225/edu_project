package com.example.edubackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RemindStudentsDTO {
    
    private List<Long> studentIds;
}