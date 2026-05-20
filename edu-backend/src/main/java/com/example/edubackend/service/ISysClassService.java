package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.edubackend.dto.ClassVO;
import com.example.edubackend.dto.CreateClassDTO;
import com.example.edubackend.entity.SysClass;

public interface ISysClassService extends IService<SysClass> {
    SysClass createClass(CreateClassDTO dto, Long creatorId);
    
    Page<ClassVO> getClassPage(Integer pageNum, Integer pageSize, String keyword, Byte status, Long teacherId);
    
    void assignTeacher(Long classId, Long teacherId);
    
    void removeTeacher(Long classId, Long teacherId);
}
