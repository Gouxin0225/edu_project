package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleCampusGradeDTO {
    private Long id;
    private String schoolName;
    private String gradeName;
    private Long classId;
    private String majorDirection;
    private Integer studentCount;
    private LocalDate startDate;
    private LocalDate expectedGraduateDate;
    private String status;
}
