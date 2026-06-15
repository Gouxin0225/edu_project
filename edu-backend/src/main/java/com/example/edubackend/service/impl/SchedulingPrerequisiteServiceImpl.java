package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.SchedulingPrerequisiteResultVO;
import com.example.edubackend.entity.ScheduleCampusCourseProgress;
import com.example.edubackend.entity.ScheduleCampusGrade;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleCampusCourseProgressMapper;
import com.example.edubackend.mapper.ScheduleCampusGradeMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.service.SchedulingPrerequisiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingPrerequisiteServiceImpl implements SchedulingPrerequisiteService {

    private static final String COMPLETED = "COMPLETED";
    private static final Set<String> ADVANCED_COURSE_CODES = Set.of(
            "PENETRATION_TEST", "DEFENSE_PROTECTION", "CLOUD_COMPUTING"
    );
    private static final List<String> ADVANCED_REQUIRED_COURSE_CODES = List.of(
            "HCIA", "PYTHON", "RHCSA", "WEB", "MYSQL", "HCIP", "RHCE"
    );

    private final ScheduleCourseMapper scheduleCourseMapper;
    private final ScheduleCampusGradeMapper scheduleCampusGradeMapper;
    private final ScheduleCampusCourseProgressMapper scheduleCampusCourseProgressMapper;

    @Override
    public SchedulingPrerequisiteResultVO check(Long campusId, Long gradeId, Long courseId) {
        if (gradeId == null) {
            throw new BusinessException(400, "年级ID不能为空");
        }
        if (courseId == null) {
            throw new BusinessException(400, "课程ID不能为空");
        }
        ScheduleCampusGrade grade = scheduleCampusGradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BusinessException(404, "校区年级不存在");
        }
        ScheduleCourse targetCourse = scheduleCourseMapper.selectById(courseId);
        if (targetCourse == null) {
            throw new BusinessException(404, "课程不存在");
        }

        String courseCode = targetCourse.getCourseCode();
        if ("HCIP".equals(courseCode)) {
            return checkRequiredCourses(gradeId, List.of("HCIA"));
        }
        if ("RHCE".equals(courseCode)) {
            return checkRequiredCourses(gradeId, List.of("RHCSA"));
        }
        if (ADVANCED_COURSE_CODES.contains(courseCode)) {
            return checkRequiredCourses(gradeId, ADVANCED_REQUIRED_COURSE_CODES);
        }
        return SchedulingPrerequisiteResultVO.passed();
    }

    private SchedulingPrerequisiteResultVO checkRequiredCourses(Long gradeId, List<String> requiredCourseCodes) {
        Map<String, ScheduleCourse> courseMap = scheduleCourseMapper.selectList(new LambdaQueryWrapper<ScheduleCourse>()
                        .in(ScheduleCourse::getCourseCode, requiredCourseCodes))
                .stream()
                .collect(Collectors.toMap(ScheduleCourse::getCourseCode, Function.identity()));
        Map<Long, ScheduleCampusCourseProgress> progressMap = loadProgressMap(gradeId, courseMap.values().stream()
                .map(ScheduleCourse::getId)
                .toList());

        List<String> blockedReasons = new ArrayList<>();
        for (String requiredCode : requiredCourseCodes) {
            ScheduleCourse requiredCourse = courseMap.get(requiredCode);
            if (requiredCourse == null) {
                blockedReasons.add("前置课程未配置: " + requiredCode);
                continue;
            }
            ScheduleCampusCourseProgress progress = progressMap.get(requiredCourse.getId());
            if (progress == null) {
                blockedReasons.add("前置课程未完成: " + requiredCourse.getCourseName());
                continue;
            }
            if (!COMPLETED.equals(progress.getProgressStatus())) {
                blockedReasons.add("前置课程未完成: " + requiredCourse.getCourseName());
            }
        }
        return blockedReasons.isEmpty()
                ? SchedulingPrerequisiteResultVO.passed()
                : SchedulingPrerequisiteResultVO.blocked(blockedReasons);
    }

    private Map<Long, ScheduleCampusCourseProgress> loadProgressMap(Long gradeId, List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return scheduleCampusCourseProgressMapper.selectList(new LambdaQueryWrapper<ScheduleCampusCourseProgress>()
                        .eq(ScheduleCampusCourseProgress::getCampusGradeId, gradeId)
                        .in(ScheduleCampusCourseProgress::getCourseId, courseIds))
                .stream()
                .collect(Collectors.toMap(ScheduleCampusCourseProgress::getCourseId, Function.identity(), (a, b) -> a));
    }
}
