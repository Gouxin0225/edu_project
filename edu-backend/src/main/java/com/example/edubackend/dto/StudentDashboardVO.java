package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentDashboardVO {
    
    private List<TaskVO> upcomingTasks;
    private List<TaskVO> pendingTasks;
    private List<TaskVO> completedTasks;
    
    @Data
    public static class TaskVO {
        private Long taskId;
        private String taskType;
        private String title;
        private LocalDateTime deadline;
        private String status;
        private Integer totalQuestions;
        private Integer scoreGained;
    }
}