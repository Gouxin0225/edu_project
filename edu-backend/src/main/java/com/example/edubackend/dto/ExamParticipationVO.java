package com.example.edubackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamParticipationVO {
    private Long examId;
    private String examTitle;
    private List<StudentInfo> submittedStudents;
    private List<StudentInfo> notSubmittedStudents;
    
    @Data
    public static class StudentInfo {
        private Long studentId;
        private String studentName;
        private String submitTime;
        private String status;
    }
}
