package com.example.edubackend.dto;

import lombok.Data;

@Data
public class ScheduleCampusProgressUpdateDTO {
    private Integer expectedStudentCount;
    private String progressStatus;
    private String remark;
}
