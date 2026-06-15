package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.edubackend.dto.ScheduleCourseDTO;
import com.example.edubackend.dto.ScheduleCourseVO;
import com.example.edubackend.entity.ScheduleCourse;

import java.util.List;

public interface IScheduleCourseService extends IService<ScheduleCourse> {
    Page<ScheduleCourseVO> getCoursePage(Integer pageNum, Integer pageSize, String keyword,
                                         String courseLevel, String courseType, String status);

    ScheduleCourseVO createCourse(ScheduleCourseDTO dto);

    ScheduleCourseVO updateCourse(Long id, ScheduleCourseDTO dto);

    void updateStatus(Long id, String status);

    List<ScheduleCourseVO> initFixedCourses();
}
