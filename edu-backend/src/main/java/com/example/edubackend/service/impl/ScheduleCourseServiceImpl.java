package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.dto.ScheduleCourseDTO;
import com.example.edubackend.dto.ScheduleCourseVO;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.service.IScheduleCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleCourseServiceImpl extends ServiceImpl<ScheduleCourseMapper, ScheduleCourse>
        implements IScheduleCourseService {

    private static final Set<String> COURSE_LEVELS = Set.of("BASIC", "INTERMEDIATE", "ADVANCED");
    private static final Set<String> COURSE_TYPES = Set.of("FIXED", "CUSTOM");
    private static final Set<String> COURSE_STATUSES = Set.of("ACTIVE", "DISABLED");

    private static final List<FixedCourse> FIXED_COURSES = List.of(
            new FixedCourse("HCIA", "HCIA", "NETWORK", "BASIC", 10, "基础课程"),
            new FixedCourse("PYTHON", "Python", "DEV", "BASIC", 20, "基础课程"),
            new FixedCourse("RHCSA", "RHCSA", "LINUX", "BASIC", 30, "基础课程"),
            new FixedCourse("WEB", "Web", "WEB", "BASIC", 40, "基础课程"),
            new FixedCourse("MYSQL", "MySQL", "DB", "BASIC", 50, "基础课程"),
            new FixedCourse("HCIP", "HCIP", "NETWORK", "INTERMEDIATE", 110, "中级课程"),
            new FixedCourse("RHCE", "RHCE", "LINUX", "INTERMEDIATE", 120, "中级课程"),
            new FixedCourse("PENETRATION_TEST", "渗透测试", "SECURITY", "ADVANCED", 210, "高级课程"),
            new FixedCourse("DEFENSE_PROTECTION", "防御保护", "SECURITY", "ADVANCED", 220, "高级课程"),
            new FixedCourse("CLOUD_COMPUTING", "云计算", "CLOUD", "ADVANCED", 230, "高级课程")
    );

    @Override
    public Page<ScheduleCourseVO> getCoursePage(Integer pageNum, Integer pageSize, String keyword,
                                                String courseLevel, String courseType, String status) {
        Page<ScheduleCourse> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<ScheduleCourse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(ScheduleCourse::getCourseName, trimmed)
                    .or()
                    .like(ScheduleCourse::getCourseCode, trimmed));
        }
        if (StringUtils.hasText(courseLevel)) {
            wrapper.eq(ScheduleCourse::getCourseLevel, normalize(courseLevel));
        }
        if (StringUtils.hasText(courseType)) {
            wrapper.eq(ScheduleCourse::getCourseType, normalize(courseType));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ScheduleCourse::getStatus, normalize(status));
        }
        wrapper.orderByAsc(ScheduleCourse::getSortOrder).orderByDesc(ScheduleCourse::getCreateTime);

        Page<ScheduleCourse> result = baseMapper.selectPage(page, wrapper);
        Page<ScheduleCourseVO> voPage = new Page<>(page.getCurrent(), page.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public ScheduleCourseVO createCourse(ScheduleCourseDTO dto) {
        ScheduleCourse course = new ScheduleCourse();
        applyDto(course, dto);
        course.setStatus(StringUtils.hasText(dto.getStatus()) ? validateStatus(dto.getStatus()) : "ACTIVE");
        ensureCourseCodeUnique(course.getCourseCode(), null);
        baseMapper.insert(course);
        return toVO(course);
    }

    @Override
    @Transactional
    public ScheduleCourseVO updateCourse(Long id, ScheduleCourseDTO dto) {
        ScheduleCourse course = getExistingCourse(id);
        applyDto(course, dto);
        if (StringUtils.hasText(dto.getStatus())) {
            course.setStatus(validateStatus(dto.getStatus()));
        }
        ensureCourseCodeUnique(course.getCourseCode(), id);
        baseMapper.updateById(course);
        return toVO(getExistingCourse(id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        ScheduleCourse course = getExistingCourse(id);
        course.setStatus(validateStatus(status));
        baseMapper.updateById(course);
    }

    @Override
    @Transactional
    public List<ScheduleCourseVO> initFixedCourses() {
        for (FixedCourse fixed : FIXED_COURSES) {
            ScheduleCourse course = baseMapper.selectOne(new LambdaQueryWrapper<ScheduleCourse>()
                    .eq(ScheduleCourse::getCourseCode, fixed.courseCode()));
            if (course == null) {
                course = new ScheduleCourse();
                course.setCourseCode(fixed.courseCode());
                course.setStatus("ACTIVE");
            }
            course.setCourseName(fixed.courseName());
            course.setCourseDirection(fixed.courseDirection());
            course.setCourseLevel(fixed.courseLevel());
            course.setCourseType("FIXED");
            course.setTeacherCapacity(1);
            course.setSortOrder(fixed.sortOrder());
            course.setRemark(fixed.remark());
            if (course.getId() == null) {
                baseMapper.insert(course);
            } else {
                baseMapper.updateById(course);
            }
        }
        return baseMapper.selectList(new LambdaQueryWrapper<ScheduleCourse>()
                        .eq(ScheduleCourse::getCourseType, "FIXED")
                        .orderByAsc(ScheduleCourse::getSortOrder))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private void applyDto(ScheduleCourse course, ScheduleCourseDTO dto) {
        course.setCourseCode(required(dto.getCourseCode(), "课程编码不能为空").toUpperCase(Locale.ROOT));
        course.setCourseName(required(dto.getCourseName(), "课程名称不能为空"));
        course.setCourseDirection(required(dto.getCourseDirection(), "课程方向不能为空").toUpperCase(Locale.ROOT));
        course.setCourseLevel(validateCourseLevel(dto.getCourseLevel()));
        course.setCourseType(validateCourseType(dto.getCourseType()));
        if (dto.getTeacherCapacity() == null || dto.getTeacherCapacity() <= 0) {
            throw new BusinessException(400, "讲师承载人数必须大于0");
        }
        course.setTeacherCapacity(dto.getTeacherCapacity());
        course.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        course.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
    }

    private ScheduleCourse getExistingCourse(Long id) {
        if (id == null) {
            throw new BusinessException(400, "课程ID不能为空");
        }
        ScheduleCourse course = baseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
    }

    private void ensureCourseCodeUnique(String courseCode, Long excludeId) {
        LambdaQueryWrapper<ScheduleCourse> wrapper = new LambdaQueryWrapper<ScheduleCourse>()
                .eq(ScheduleCourse::getCourseCode, courseCode);
        if (excludeId != null) {
            wrapper.ne(ScheduleCourse::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "课程编码已存在");
        }
    }

    private String validateCourseLevel(String courseLevel) {
        String normalized = normalize(courseLevel);
        if (!COURSE_LEVELS.contains(normalized)) {
            throw new BusinessException(400, "课程层级不合法");
        }
        return normalized;
    }

    private String validateCourseType(String courseType) {
        String normalized = normalize(courseType);
        if (!COURSE_TYPES.contains(normalized)) {
            throw new BusinessException(400, "课程类型不合法");
        }
        return normalized;
    }

    private String validateStatus(String status) {
        String normalized = normalize(status);
        if (!COURSE_STATUSES.contains(normalized)) {
            throw new BusinessException(400, "课程状态不合法");
        }
        return normalized;
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private String normalize(String value) {
        return required(value, "参数不能为空").toUpperCase(Locale.ROOT);
    }

    private ScheduleCourseVO toVO(ScheduleCourse course) {
        ScheduleCourseVO vo = new ScheduleCourseVO();
        vo.setId(course.getId());
        vo.setCourseCode(course.getCourseCode());
        vo.setCourseName(course.getCourseName());
        vo.setCourseDirection(course.getCourseDirection());
        vo.setCourseLevel(course.getCourseLevel());
        vo.setCourseType(course.getCourseType());
        vo.setTeacherCapacity(course.getTeacherCapacity());
        vo.setSortOrder(course.getSortOrder());
        vo.setStatus(course.getStatus());
        vo.setRemark(course.getRemark());
        vo.setCreateTime(course.getCreateTime());
        vo.setUpdateTime(course.getUpdateTime());
        return vo;
    }

    private record FixedCourse(String courseCode, String courseName, String courseDirection,
                               String courseLevel, Integer sortOrder, String remark) {
    }
}
