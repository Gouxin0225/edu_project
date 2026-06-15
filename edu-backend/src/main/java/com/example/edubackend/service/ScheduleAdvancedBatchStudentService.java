package com.example.edubackend.service;

import com.example.edubackend.dto.ScheduleAdvancedBatchStudentImportDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchStudentVO;

import java.util.List;

public interface ScheduleAdvancedBatchStudentService {
    List<ScheduleAdvancedBatchStudentVO> listStudents(Long batchId);

    List<ScheduleAdvancedBatchStudentVO> importStudents(Long batchId, ScheduleAdvancedBatchStudentImportDTO dto);

    List<ScheduleAdvancedBatchStudentVO> checkEligibility(Long batchId);
}
