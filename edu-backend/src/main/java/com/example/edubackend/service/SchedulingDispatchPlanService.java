package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.ScheduleDispatchPlanCreateDTO;
import com.example.edubackend.dto.ScheduleDispatchPlanVO;

public interface SchedulingDispatchPlanService {
    Page<ScheduleDispatchPlanVO> getDispatchPlans(Integer pageNum, Integer pageSize, Long campusGradeId,
                                                  Long courseId, String planStatus, String keyword);

    ScheduleDispatchPlanVO createDispatchPlan(ScheduleDispatchPlanCreateDTO dto);

    ScheduleDispatchPlanVO confirm(Long id);

    ScheduleDispatchPlanVO start(Long id);

    ScheduleDispatchPlanVO cancel(Long id);
}
