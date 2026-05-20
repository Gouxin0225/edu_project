package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.edubackend.entity.TeacherClassRel;

import java.util.List;

public interface ITeacherClassRelService extends IService<TeacherClassRel> {
    List<Long> getTeacherClassIds(Long teacherId);
    
    List<Long> getClassTeacherIds(Long classId);
}