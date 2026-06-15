package com.example.edubackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SchedulingMatchTeachersVO {
    private Integer suggestedTeacherCount;
    private List<TeacherCandidate> teacherCandidates = new ArrayList<>();
    private List<Long> recommendedTeacherIds = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    public static class TeacherCandidate {
        private Long teacherId;
        private String teacherName;
        private String username;
        private Long skillId;
        private String skillLevel;
        private String dispatchStatus;
        private Byte canTravel;
        private Byte isCenterReserve;
        private Boolean reserveRisk;
        private Integer recentDispatchCount;
        private Integer score;
        private List<String> reasons = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
    }
}
