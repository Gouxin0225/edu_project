package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.entity.StudentAnswerDetail;
import com.example.edubackend.mapper.StudentAnswerDetailMapper;
import com.example.edubackend.service.IStudentAnswerDetailService;
import org.springframework.stereotype.Service;

@Service
public class StudentAnswerDetailServiceImpl extends ServiceImpl<StudentAnswerDetailMapper, StudentAnswerDetail> implements IStudentAnswerDetailService {

    @Override
    public boolean saveOrUpdate(StudentAnswerDetail entity) {
        LambdaQueryWrapper<StudentAnswerDetail> wrapper = new LambdaQueryWrapper<StudentAnswerDetail>()
                .eq(StudentAnswerDetail::getSubmissionId, entity.getSubmissionId())
                .eq(StudentAnswerDetail::getQuestionId, entity.getQuestionId());
        StudentAnswerDetail existing = getOne(wrapper);
        if (existing != null) {
            entity.setId(existing.getId());
            return updateById(entity);
        } else {
            return save(entity);
        }
    }
}
