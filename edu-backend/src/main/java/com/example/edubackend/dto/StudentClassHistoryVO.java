package com.example.edubackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentClassHistoryVO {
    private Long classId;
    private String className;
    private String status; // sys_class.status
    private String joinStatus; // class_student_rel.status
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
}
