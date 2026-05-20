package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassJoinApplicationVO {
    private Long id;
    private Long classId;
    private String className;
    private Long studentId;
    private String studentName;
    private String username;
    private String phone;
    private String schoolName;
    private String status;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
    private Long auditorId;
    private String auditorName;
    private String rejectReason;
}
