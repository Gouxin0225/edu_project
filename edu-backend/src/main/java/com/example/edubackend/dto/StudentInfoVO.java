package com.example.edubackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String schoolName;
    private String role;
    private Long classId; // current classId in sys_user
    private String className; // current className
    private LocalDateTime createTime;
    private List<StudentClassHistoryVO> classHistory;
}
