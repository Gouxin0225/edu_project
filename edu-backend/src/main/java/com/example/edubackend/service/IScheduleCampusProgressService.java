package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.ScheduleCampusCourseProgressVO;
import com.example.edubackend.dto.ScheduleCampusProgressInitDTO;

import java.util.List;

public interface IScheduleCampusProgressService {
    Page<ScheduleCampusCourseProgressVO> getProgressPage(Integer pageNum, Integer pageSize, String schoolName,
                                                         Long campusGradeId, Long courseId, String progressStatus,
                                                         String keyword);

    Page<ScheduleCampusCourseProgressVO> getMySchoolProgress(Integer pageNum, Integer pageSize, Long campusGradeId,
                                                             Long courseId, String progressStatus, String keyword);

    List<ScheduleCampusCourseProgressVO> initGradeCourses(ScheduleCampusProgressInitDTO dto);

    ScheduleCampusCourseProgressVO updateExpectedStudentCount(Long id, Integer expectedStudentCount);

    ScheduleCampusCourseProgressVO updateRemark(Long id, String remark);

    ScheduleCampusCourseProgressVO updateStatus(Long id, String progressStatus);
}
