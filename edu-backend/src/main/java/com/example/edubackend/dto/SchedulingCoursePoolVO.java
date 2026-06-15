package com.example.edubackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SchedulingCoursePoolVO {
    private Long progressId;
    private Long campusGradeId;
    private String schoolName;
    private String gradeName;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String courseDirection;
    private String courseLevel;
    private Integer expectedStudentCount;
    private String progressStatus;
    private Boolean canSchedule;
    private List<String> blockedReasons = new ArrayList<>();
    private Integer teacherCapacity;
    private Integer suggestedTeacherCount;
}
