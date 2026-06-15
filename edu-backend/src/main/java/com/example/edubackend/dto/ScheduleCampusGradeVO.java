package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleCampusGradeVO {
    private Long id;
    private String schoolName;
    private String gradeName;
    private Long classId;
    private String className;
    private String majorDirection;
    private Integer studentCount;
    private LocalDate startDate;
    private LocalDate expectedGraduateDate;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
