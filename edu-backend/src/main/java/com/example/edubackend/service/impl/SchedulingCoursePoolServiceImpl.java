package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.SchedulingCoursePoolVO;
import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.service.SchedulingCoursePoolService;
import com.example.edubackend.service.SchedulingPrerequisiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingCoursePoolServiceImpl implements SchedulingCoursePoolService {

    private static final List<String> POOL_STATUSES = List.of("NOT_STARTED", "NEED_MAKEUP");

    private final ScheduleCampusCourseProgressMapper scheduleCampusCourseProgressMapper;
    private final ScheduleCampusGradeMapper scheduleCampusGradeMapper;
    private final ScheduleCourseMapper scheduleCourseMapper;
    private final SchedulingPrerequisiteService schedulingPrerequisiteService;

    @Override
    public Page<SchedulingCoursePoolVO> getCoursePool(Integer pageNum, Integer pageSize, String schoolName,
                                                      Long campusGradeId, Long courseId, String keyword) {
        List<Long> gradeIds = loadGradeIds(schoolName, campusGradeId, keyword);
        if ((StringUtils.hasText(schoolName) || campusGradeId != null || StringUtils.hasText(keyword)) && gradeIds.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        Page<ScheduleCampusCourseProgress> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<ScheduleCampusCourseProgress> wrapper = new LambdaQueryWrapper<ScheduleCampusCourseProgress>()
                .in(ScheduleCampusCourseProgress::getProgressStatus, POOL_STATUSES);
        if (!gradeIds.isEmpty()) {
            wrapper.in(ScheduleCampusCourseProgress::getCampusGradeId, gradeIds);
        }
        if (courseId != null) {
            wrapper.eq(ScheduleCampusCourseProgress::getCourseId, courseId);
        }
        wrapper.orderByAsc(ScheduleCampusCourseProgress::getCampusGradeId)
                .orderByAsc(ScheduleCampusCourseProgress::getCourseId);

        Page<ScheduleCampusCourseProgress> result = scheduleCampusCourseProgressMapper.selectPage(page, wrapper);
        Page<SchedulingCoursePoolVO> voPage = new Page<>(page.getCurrent(), page.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(toVOList(result.getRecords()));
        return voPage;
    }

    private List<Long> loadGradeIds(String schoolName, Long campusGradeId, String keyword) {
        LambdaQueryWrapper<ScheduleCampusGrade> wrapper = new LambdaQueryWrapper<ScheduleCampusGrade>()
                .eq(ScheduleCampusGrade::getIsDeleted, (byte) 0);
        if (StringUtils.hasText(schoolName)) {
            wrapper.eq(ScheduleCampusGrade::getSchoolName, schoolName.trim());
        }
        if (campusGradeId != null) {
            wrapper.eq(ScheduleCampusGrade::getId, campusGradeId);
        }
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(ScheduleCampusGrade::getSchoolName, trimmed)
                    .or()
                    .like(ScheduleCampusGrade::getGradeName, trimmed));
        }
        return scheduleCampusGradeMapper.selectList(wrapper).stream()
                .map(ScheduleCampusGrade::getId)
                .toList();
    }

    private List<SchedulingCoursePoolVO> toVOList(List<ScheduleCampusCourseProgress> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Set<Long> gradeIds = records.stream().map(ScheduleCampusCourseProgress::getCampusGradeId).collect(Collectors.toSet());
        Set<Long> courseIds = records.stream().map(ScheduleCampusCourseProgress::getCourseId).collect(Collectors.toSet());
        Map<Long, ScheduleCampusGrade> gradeMap = scheduleCampusGradeMapper.selectBatchIds(gradeIds).stream()
                .collect(Collectors.toMap(ScheduleCampusGrade::getId, Function.identity()));
        Map<Long, ScheduleCourse> courseMap = scheduleCourseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(ScheduleCourse::getId, Function.identity()));

        return records.stream().map(progress -> {
            ScheduleCampusGrade grade = gradeMap.get(progress.getCampusGradeId());
            ScheduleCourse course = courseMap.get(progress.getCourseId());
            SchedulingCoursePoolVO vo = new SchedulingCoursePoolVO();
            vo.setProgressId(progress.getId());
            vo.setCampusGradeId(progress.getCampusGradeId());
            vo.setSchoolName(grade != null ? grade.getSchoolName() : null);
            vo.setGradeName(grade != null ? grade.getGradeName() : null);
            vo.setCourseId(progress.getCourseId());
            if (course != null) {
                vo.setCourseCode(course.getCourseCode());
                vo.setCourseName(course.getCourseName());
                vo.setCourseDirection(course.getCourseDirection());
                vo.setCourseLevel(course.getCourseLevel());
                vo.setTeacherCapacity(course.getTeacherCapacity());
            }
            vo.setExpectedStudentCount(progress.getExpectedStudentCount());
            vo.setProgressStatus(progress.getProgressStatus());
            SchedulingPrerequisiteResultVO prerequisite = schedulingPrerequisiteService.check(
                    null, progress.getCampusGradeId(), progress.getCourseId());
            vo.setCanSchedule(prerequisite.getCanSchedule());
            vo.setBlockedReasons(prerequisite.getBlockedReasons());
            vo.setSuggestedTeacherCount(calculateSuggestedTeacherCount(progress.getExpectedStudentCount(),
                    course != null ? course.getTeacherCapacity() : null));
            return vo;
        }).toList();
    }

    private Integer calculateSuggestedTeacherCount(Integer expectedStudentCount, Integer teacherCapacity) {
        if (expectedStudentCount == null || expectedStudentCount <= 0 || teacherCapacity == null || teacherCapacity <= 0) {
            return 1;
        }
        return (expectedStudentCount + teacherCapacity - 1) / teacherCapacity;
    }

    private Page<SchedulingCoursePoolVO> emptyPage(Integer pageNum, Integer pageSize) {
        Page<SchedulingCoursePoolVO> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        page.setTotal(0);
        page.setRecords(List.of());
        return page;
    }
}
