package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.ScheduleAdvancedBatchDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchVO;

public interface ScheduleAdvancedBatchService {
    Page<ScheduleAdvancedBatchVO> getBatches(Integer pageNum, Integer pageSize, Long courseId,
                                             String schoolName, String batchStatus, String keyword);

    ScheduleAdvancedBatchVO createBatch(ScheduleAdvancedBatchDTO dto);
}
