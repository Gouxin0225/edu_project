package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ClassVO {
    private Long id;
    private String className;
    private Byte status;
    private String statusName;
    private String schoolName;
    private Byte allowStudentApply;
    private LocalDateTime createTime;
    private Integer teacherCount;
    private List<Long> teacherIds = new ArrayList<>();
    private List<String> teacherNames = new ArrayList<>();
    private Integer assistantCount;
    private List<Long> assistantIds = new ArrayList<>();
    private List<String> assistantNames = new ArrayList<>();
}
