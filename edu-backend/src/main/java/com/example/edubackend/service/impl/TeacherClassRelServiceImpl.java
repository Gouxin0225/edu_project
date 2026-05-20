package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.service.ITeacherClassRelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherClassRelServiceImpl extends ServiceImpl<TeacherClassRelMapper, TeacherClassRel> implements ITeacherClassRelService {

    @Override
    public List<Long> getTeacherClassIds(Long teacherId) {
        LambdaQueryWrapper<TeacherClassRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClassRel::getTeacherId, teacherId);
        return baseMapper.selectList(wrapper).stream()
                .map(TeacherClassRel::getClassId)
                .toList();
    }

    @Override
    public List<Long> getClassTeacherIds(Long classId) {
        LambdaQueryWrapper<TeacherClassRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClassRel::getClassId, classId);
        return baseMapper.selectList(wrapper).stream()
                .map(TeacherClassRel::getTeacherId)
                .toList();
    }
}