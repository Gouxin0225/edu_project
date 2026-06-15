package com.example.edubackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.ScheduleTeacherDispatchVO;
import com.example.edubackend.dto.ScheduleTeacherProfileConfigDTO;
import com.example.edubackend.dto.ScheduleTeacherSkillConfigDTO;

import java.util.List;

public interface IScheduleTeacherCapabilityService {
    Page<ScheduleTeacherDispatchVO> getTeacherProfiles(Integer pageNum, Integer pageSize, String keyword,
                                                       String dispatchStatus, Long courseId,
                                                       Byte canTravel, Byte isCenterReserve);

    ScheduleTeacherDispatchVO saveProfile(ScheduleTeacherProfileConfigDTO dto);

    ScheduleTeacherDispatchVO saveTeacherSkills(ScheduleTeacherSkillConfigDTO dto);

    List<ScheduleTeacherDispatchVO> getAvailableTeachersByCourse(Long courseId, String skillLevel);
}
