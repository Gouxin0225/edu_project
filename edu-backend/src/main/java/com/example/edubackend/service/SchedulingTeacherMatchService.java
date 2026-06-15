package com.example.edubackend.service;

import com.example.edubackend.dto.SchedulingMatchTeachersDTO;
import com.example.edubackend.dto.SchedulingMatchTeachersVO;

public interface SchedulingTeacherMatchService {
    SchedulingMatchTeachersVO matchTeachers(SchedulingMatchTeachersDTO dto);
}
