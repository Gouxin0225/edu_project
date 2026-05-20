package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class StudentClassVO {
    private Long classId;
    private String className;
    private Byte status;
    private String schoolName;
    private Boolean allowStudentApply;
    private Boolean joined;
    private String applicationStatus;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
    private String rejectReason;
    private List<String> teacherNames = new ArrayList<>();
    private List<String> assistantNames = new ArrayList<>();
}
