package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.SchedulingCoursePoolVO;

public interface SchedulingCoursePoolService {
    Page<SchedulingCoursePoolVO> getCoursePool(Integer pageNum, Integer pageSize, String schoolName,
                                               Long campusGradeId, Long courseId, String keyword);
}
