package com.example.edubackend.service;

import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;

public interface SchedulingPrerequisiteService {
    SchedulingPrerequisiteResultVO check(Long campusId, Long gradeId, Long courseId);
}
