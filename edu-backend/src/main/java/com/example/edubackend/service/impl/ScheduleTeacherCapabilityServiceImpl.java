package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.ScheduleTeacherDispatchVO;
import com.example.edubackend.dto.ScheduleTeacherProfileConfigDTO;
import com.example.edubackend.dto.ScheduleTeacherSkillConfigDTO;
import com.example.edubackend.dto.ScheduleTeacherSkillVO;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.ScheduleTeacherProfile;
import com.example.edubackend.entity.ScheduleTeacherSkill;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.ScheduleTeacherProfileMapper;
import com.example.edubackend.mapper.ScheduleTeacherSkillMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.IScheduleTeacherCapabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleTeacherCapabilityServiceImpl implements IScheduleTeacherCapabilityService {

    private static final Set<String> SKILL_LEVELS = Set.of("MAIN", "TEACHABLE", "ASSIST");
    private static final Set<String> DISPATCH_STATUSES = Set.of("AVAILABLE", "PAUSED", "UNAVAILABLE");
    private static final Set<String> SKILL_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final SysUserMapper sysUserMapper;
    private final ScheduleCourseMapper scheduleCourseMapper;
    private final ScheduleTeacherProfileMapper scheduleTeacherProfileMapper;
    private final ScheduleTeacherSkillMapper scheduleTeacherSkillMapper;

    @Override
    public Page<ScheduleTeacherDispatchVO> getTeacherProfiles(Integer pageNum, Integer pageSize, String keyword,
                                                              String dispatchStatus, Long courseId,
                                                              Byte canTravel, Byte isCenterReserve) {
        List<Long> filteredTeacherIds = null;
        if (courseId != null) {
            filteredTeacherIds = scheduleTeacherSkillMapper.selectList(new LambdaQueryWrapper<ScheduleTeacherSkill>()
                            .eq(ScheduleTeacherSkill::getCourseId, courseId)
                            .eq(ScheduleTeacherSkill::getStatus, "ACTIVE"))
                    .stream()
                    .map(ScheduleTeacherSkill::getTeacherId)
                    .distinct()
                    .toList();
            if (filteredTeacherIds.isEmpty()) {
                return emptyPage(pageNum, pageSize);
            }
        }

        Page<SysUser> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "TEACHER")
                .eq(SysUser::getIsDeleted, (byte) 0);
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, trimmed)
                    .or()
                    .like(SysUser::getRealName, trimmed)
                    .or()
                    .like(SysUser::getPhone, trimmed));
        }
        if (filteredTeacherIds != null) {
            wrapper.in(SysUser::getId, filteredTeacherIds);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);

        List<ScheduleTeacherDispatchVO> records = buildTeacherVOs(userPage.getRecords());
        if (StringUtils.hasText(dispatchStatus) || canTravel != null || isCenterReserve != null) {
            String normalizedStatus = StringUtils.hasText(dispatchStatus) ? normalizeDispatchStatus(dispatchStatus) : null;
            records = records.stream()
                    .filter(vo -> normalizedStatus == null || normalizedStatus.equals(vo.getDispatchStatus()))
                    .filter(vo -> canTravel == null || canTravel.equals(vo.getCanTravel()))
                    .filter(vo -> isCenterReserve == null || isCenterReserve.equals(vo.getIsCenterReserve()))
                    .toList();
        }

        Page<ScheduleTeacherDispatchVO> result = new Page<>(page.getCurrent(), page.getSize());
        result.setTotal(userPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public ScheduleTeacherDispatchVO saveProfile(ScheduleTeacherProfileConfigDTO dto) {
        SysUser teacher = getTeacher(dto.getTeacherId());
        ScheduleTeacherProfile profile = getOrCreateProfile(teacher.getId());
        profile.setHomeSchoolName(StringUtils.hasText(dto.getHomeSchoolName()) ? dto.getHomeSchoolName().trim() : null);
        profile.setDispatchMode("NORMAL");
        profile.setDispatchStatus(StringUtils.hasText(dto.getDispatchStatus())
                ? normalizeDispatchStatus(dto.getDispatchStatus())
                : defaultString(profile.getDispatchStatus(), "AVAILABLE"));
        profile.setCanTravel(dto.getCanTravel() != null ? dto.getCanTravel() : defaultByte(profile.getCanTravel(), (byte) 1));
        profile.setAllowCrossSchool(profile.getCanTravel());
        profile.setIsCenterReserve(dto.getIsCenterReserve() != null ? dto.getIsCenterReserve() : defaultByte(profile.getIsCenterReserve(), (byte) 0));
        profile.setMaxParallelPlans(dto.getMaxParallelPlans() != null ? dto.getMaxParallelPlans() : defaultInt(profile.getMaxParallelPlans(), 1));
        profile.setMaxMonthlyDispatchDays(dto.getMaxMonthlyDispatchDays());
        profile.setPriority(dto.getPriority() != null ? dto.getPriority() : defaultInt(profile.getPriority(), 100));
        profile.setAvailableRemark(StringUtils.hasText(dto.getAvailableRemark()) ? dto.getAvailableRemark().trim() : null);
        profile.setStatus("ACTIVE");
        if (profile.getId() == null) {
            scheduleTeacherProfileMapper.insert(profile);
        } else {
            scheduleTeacherProfileMapper.updateById(profile);
        }
        return buildTeacherVOs(List.of(teacher)).get(0);
    }

    @Override
    @Transactional
    public ScheduleTeacherDispatchVO saveTeacherSkills(ScheduleTeacherSkillConfigDTO dto) {
        SysUser teacher = getTeacher(dto.getTeacherId());
        getOrCreateActiveProfile(teacher.getId());
        Set<Long> courseIds = dto.getCourses().stream()
                .map(ScheduleTeacherSkillConfigDTO.CourseSkillItem::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, ScheduleCourse> courseMap = loadCourseMap(courseIds);
        if (courseMap.size() != courseIds.size()) {
            throw new BusinessException(404, "存在不存在的课程");
        }

        for (ScheduleTeacherSkillConfigDTO.CourseSkillItem item : dto.getCourses()) {
            ScheduleTeacherSkill skill = scheduleTeacherSkillMapper.selectOne(new LambdaQueryWrapper<ScheduleTeacherSkill>()
                    .eq(ScheduleTeacherSkill::getTeacherId, teacher.getId())
                    .eq(ScheduleTeacherSkill::getCourseId, item.getCourseId()));
            if (skill == null) {
                skill = new ScheduleTeacherSkill();
                skill.setTeacherId(teacher.getId());
                skill.setCourseId(item.getCourseId());
            }
            skill.setSkillLevel(normalizeSkillLevel(item.getSkillLevel()));
            skill.setCertified(item.getCertified() != null ? item.getCertified() : (byte) 0);
            skill.setStatus(StringUtils.hasText(item.getStatus()) ? normalizeSkillStatus(item.getStatus()) : "ACTIVE");
            skill.setRemark(StringUtils.hasText(item.getRemark()) ? item.getRemark().trim() : null);
            if (skill.getId() == null) {
                scheduleTeacherSkillMapper.insert(skill);
            } else {
                scheduleTeacherSkillMapper.updateById(skill);
            }
        }
        return buildTeacherVOs(List.of(teacher)).get(0);
    }

    @Override
    public List<ScheduleTeacherDispatchVO> getAvailableTeachersByCourse(Long courseId, String skillLevel) {
        if (courseId == null || scheduleCourseMapper.selectById(courseId) == null) {
            throw new BusinessException(404, "课程不存在");
        }
        LambdaQueryWrapper<ScheduleTeacherSkill> skillWrapper = new LambdaQueryWrapper<ScheduleTeacherSkill>()
                .eq(ScheduleTeacherSkill::getCourseId, courseId)
                .eq(ScheduleTeacherSkill::getStatus, "ACTIVE");
        if (StringUtils.hasText(skillLevel)) {
            skillWrapper.eq(ScheduleTeacherSkill::getSkillLevel, normalizeSkillLevel(skillLevel));
        }
        List<Long> teacherIds = scheduleTeacherSkillMapper.selectList(skillWrapper).stream()
                .map(ScheduleTeacherSkill::getTeacherId)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            return List.of();
        }
        List<Long> availableTeacherIds = scheduleTeacherProfileMapper.selectList(new LambdaQueryWrapper<ScheduleTeacherProfile>()
                        .in(ScheduleTeacherProfile::getTeacherId, teacherIds)
                        .eq(ScheduleTeacherProfile::getDispatchStatus, "AVAILABLE")
                        .eq(ScheduleTeacherProfile::getStatus, "ACTIVE"))
                .stream()
                .map(ScheduleTeacherProfile::getTeacherId)
                .toList();
        if (availableTeacherIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> teachers = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, availableTeacherIds)
                .eq(SysUser::getRole, "TEACHER")
                .eq(SysUser::getIsDeleted, (byte) 0));
        return buildTeacherVOs(teachers);
    }

    private List<ScheduleTeacherDispatchVO> buildTeacherVOs(List<SysUser> teachers) {
        if (teachers.isEmpty()) {
            return List.of();
        }
        List<Long> teacherIds = teachers.stream().map(SysUser::getId).toList();
        Map<Long, ScheduleTeacherProfile> profileMap = scheduleTeacherProfileMapper.selectList(
                        new LambdaQueryWrapper<ScheduleTeacherProfile>().in(ScheduleTeacherProfile::getTeacherId, teacherIds))
                .stream()
                .collect(Collectors.toMap(ScheduleTeacherProfile::getTeacherId, Function.identity(), (a, b) -> a));
        List<ScheduleTeacherSkill> skills = scheduleTeacherSkillMapper.selectList(
                new LambdaQueryWrapper<ScheduleTeacherSkill>().in(ScheduleTeacherSkill::getTeacherId, teacherIds));
        Set<Long> courseIds = skills.stream().map(ScheduleTeacherSkill::getCourseId).collect(Collectors.toSet());
        Map<Long, ScheduleCourse> courseMap = loadCourseMap(courseIds);
        Map<Long, List<ScheduleTeacherSkillVO>> skillsByTeacher = skills.stream()
                .map(skill -> toSkillVO(skill, courseMap.get(skill.getCourseId())))
                .collect(Collectors.groupingBy(ScheduleTeacherSkillVO::getTeacherId));

        return teachers.stream().map(teacher -> {
            ScheduleTeacherProfile profile = profileMap.get(teacher.getId());
            ScheduleTeacherDispatchVO vo = new ScheduleTeacherDispatchVO();
            vo.setTeacherId(teacher.getId());
            vo.setUsername(teacher.getUsername());
            vo.setTeacherName(teacher.getRealName());
            vo.setPhone(teacher.getPhone());
            vo.setSchoolName(teacher.getSchoolName());
            if (profile != null) {
                vo.setProfileId(profile.getId());
                vo.setHomeSchoolName(profile.getHomeSchoolName());
                vo.setDispatchStatus(defaultString(profile.getDispatchStatus(), "AVAILABLE"));
                vo.setCanTravel(defaultByte(profile.getCanTravel(), (byte) 1));
                vo.setIsCenterReserve(defaultByte(profile.getIsCenterReserve(), (byte) 0));
                vo.setMaxParallelPlans(profile.getMaxParallelPlans());
                vo.setMaxMonthlyDispatchDays(profile.getMaxMonthlyDispatchDays());
                vo.setPriority(profile.getPriority());
                vo.setAvailableRemark(profile.getAvailableRemark());
                vo.setCreateTime(profile.getCreateTime());
                vo.setUpdateTime(profile.getUpdateTime());
            } else {
                vo.setDispatchStatus("AVAILABLE");
                vo.setCanTravel((byte) 1);
                vo.setIsCenterReserve((byte) 0);
                vo.setMaxParallelPlans(1);
                vo.setPriority(100);
            }
            vo.setSkills(skillsByTeacher.getOrDefault(teacher.getId(), List.of()));
            return vo;
        }).toList();
    }

    private ScheduleTeacherSkillVO toSkillVO(ScheduleTeacherSkill skill, ScheduleCourse course) {
        ScheduleTeacherSkillVO vo = new ScheduleTeacherSkillVO();
        vo.setId(skill.getId());
        vo.setTeacherId(skill.getTeacherId());
        vo.setCourseId(skill.getCourseId());
        vo.setCourseName(course != null ? course.getCourseName() : null);
        vo.setSkillLevel(skill.getSkillLevel());
        vo.setCertified(skill.getCertified());
        vo.setScore(skill.getScore());
        vo.setLastTeachTime(skill.getLastTeachTime());
        vo.setStatus(skill.getStatus());
        vo.setRemark(skill.getRemark());
        vo.setCreateTime(skill.getCreateTime());
        vo.setUpdateTime(skill.getUpdateTime());
        return vo;
    }

    private SysUser getTeacher(Long teacherId) {
        if (teacherId == null) {
            throw new BusinessException(400, "讲师ID不能为空");
        }
        SysUser teacher = sysUserMapper.selectById(teacherId);
        if (teacher == null || Byte.valueOf((byte) 1).equals(teacher.getIsDeleted())) {
            throw new BusinessException(404, "讲师不存在");
        }
        if (!"TEACHER".equals(teacher.getRole())) {
            throw new BusinessException(400, "只能配置讲师用户");
        }
        return teacher;
    }

    private ScheduleTeacherProfile getOrCreateActiveProfile(Long teacherId) {
        ScheduleTeacherProfile profile = getOrCreateProfile(teacherId);
        if (profile.getId() == null) {
            profile.setDispatchMode("NORMAL");
            profile.setDispatchStatus("AVAILABLE");
            profile.setCanTravel((byte) 1);
            profile.setAllowCrossSchool((byte) 1);
            profile.setIsCenterReserve((byte) 0);
            profile.setMaxParallelPlans(1);
            profile.setPriority(100);
            profile.setStatus("ACTIVE");
            scheduleTeacherProfileMapper.insert(profile);
        }
        return profile;
    }

    private ScheduleTeacherProfile getOrCreateProfile(Long teacherId) {
        ScheduleTeacherProfile profile = scheduleTeacherProfileMapper.selectOne(new LambdaQueryWrapper<ScheduleTeacherProfile>()
                .eq(ScheduleTeacherProfile::getTeacherId, teacherId));
        if (profile == null) {
            profile = new ScheduleTeacherProfile();
            profile.setTeacherId(teacherId);
        }
        return profile;
    }

    private Map<Long, ScheduleCourse> loadCourseMap(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        return scheduleCourseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(ScheduleCourse::getId, Function.identity()));
    }

    private String normalizeSkillLevel(String skillLevel) {
        if (!StringUtils.hasText(skillLevel)) {
            return "TEACHABLE";
        }
        String value = skillLevel.trim().toUpperCase(Locale.ROOT);
        value = switch (value) {
            case "主讲", "MAIN_TEACHER" -> "MAIN";
            case "可讲", "TEACH", "CAN_TEACH" -> "TEACHABLE";
            case "辅助", "ASSISTANT" -> "ASSIST";
            default -> value;
        };
        if (!SKILL_LEVELS.contains(value)) {
            throw new BusinessException(400, "讲师能力等级不合法");
        }
        return value;
    }

    private String normalizeDispatchStatus(String status) {
        String value = normalizeRequired(status, "派课状态不能为空");
        value = switch (value) {
            case "可派" -> "AVAILABLE";
            case "暂停派课" -> "PAUSED";
            case "不可派" -> "UNAVAILABLE";
            default -> value;
        };
        if (!DISPATCH_STATUSES.contains(value)) {
            throw new BusinessException(400, "派课状态不合法");
        }
        return value;
    }

    private String normalizeSkillStatus(String status) {
        String value = normalizeRequired(status, "课程能力状态不能为空");
        if (!SKILL_STATUSES.contains(value)) {
            throw new BusinessException(400, "课程能力状态不合法");
        }
        return value;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Byte defaultByte(Byte value, Byte defaultValue) {
        return value != null ? value : defaultValue;
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value != null ? value : defaultValue;
    }

    private Page<ScheduleTeacherDispatchVO> emptyPage(Integer pageNum, Integer pageSize) {
        Page<ScheduleTeacherDispatchVO> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        page.setTotal(0);
        page.setRecords(List.of());
        return page;
    }
}
