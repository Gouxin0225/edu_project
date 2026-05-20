package com.example.edubackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class HomeworkProgressVO {
    
    private Long homeworkId;
    private String title;
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer pendingCount;
    private List<UnsubmittedStudentVO> unsubmittedStudents;
}