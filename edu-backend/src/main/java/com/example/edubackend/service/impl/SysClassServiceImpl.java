package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.dto.ClassVO;
import com.example.edubackend.dto.CreateClassDTO;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.service.ISysClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements ISysClassService {

    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public SysClass createClass(CreateClassDTO dto, Long creatorId) {
        SysClass sysClass = new SysClass();
        sysClass.setClassName(dto.getClassName());
        sysClass.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        sysClass.setSchoolName(StringUtils.hasText(dto.getSchoolName()) ? dto.getSchoolName().trim() : null);
        sysClass.setAllowStudentApply(dto.getAllowStudentApply() != null ? dto.getAllowStudentApply() : (byte) 1);
        baseMapper.insert(sysClass);
        return sysClass;
    }

    @Override
    public Page<ClassVO> getClassPage(Integer pageNum, Integer pageSize, String keyword, Byte status, Long teacherId) {
        Page<SysClass> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysClass> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysClass::getClassName, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(SysClass::getStatus, status);
        }
        if (teacherId != null) {
            List<Long> classIds = teacherClassRelMapper.selectList(
                    new LambdaQueryWrapper<TeacherClassRel>().eq(TeacherClassRel::getTeacherId, teacherId)
            ).stream()
                    .map(TeacherClassRel::getClassId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (classIds.isEmpty()) {
                Page<ClassVO> emptyPage = new Page<>(pageNum, pageSize);
                emptyPage.setTotal(0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(SysClass::getId, classIds);
        }
        wrapper.orderByDesc(SysClass::getCreateTime);
        Page<SysClass> result = baseMapper.selectPage(page, wrapper);
        
        Page<ClassVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());

        List<Long> classIds = result.getRecords().stream().map(SysClass::getId).toList();
        Map<Long, List<TeacherClassRel>> relsByClassId = loadTeacherRelations(classIds);
        Map<Long, SysUser> assignedUserMap = loadActiveAssignedUsers(relsByClassId);

        List<ClassVO> records = result.getRecords().stream().map(c -> {
            ClassVO vo = new ClassVO();
            vo.setId(c.getId());
            vo.setClassName(c.getClassName());
            vo.setStatus(c.getStatus());
            vo.setStatusName(c.getStatus() == 1 ? "进行中" : "已结课");
            vo.setSchoolName(c.getSchoolName());
            vo.setAllowStudentApply(c.getAllowStudentApply());
            vo.setCreateTime(c.getCreateTime());

            List<TeacherClassRel> rels = relsByClassId.getOrDefault(c.getId(), List.of());
            List<SysUser> assignedUsers = rels.stream()
                    .map(rel -> assignedUserMap.get(rel.getTeacherId()))
                    .filter(user -> user != null)
                    .toList();
            List<SysUser> teachers = assignedUsers.stream()
                    .filter(user -> "TEACHER".equals(user.getRole()))
                    .toList();
            List<SysUser> assistants = assignedUsers.stream()
                    .filter(user -> "ASSISTANT".equals(user.getRole()))
                    .toList();
            vo.setTeacherIds(teachers.stream().map(SysUser::getId).toList());
            vo.setTeacherNames(teachers.stream().map(SysUser::getRealName).toList());
            vo.setTeacherCount(teachers.size());
            vo.setAssistantIds(assistants.stream().map(SysUser::getId).toList());
            vo.setAssistantNames(assistants.stream().map(SysUser::getRealName).toList());
            vo.setAssistantCount(assistants.size());
            return vo;
        }).toList();
        
        voPage.setRecords(records);
        return voPage;
    }

    private Map<Long, List<TeacherClassRel>> loadTeacherRelations(List<Long> classIds) {
        classIds = classIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().in(TeacherClassRel::getClassId, classIds)
        ).stream().collect(Collectors.groupingBy(TeacherClassRel::getClassId));
    }

    private Map<Long, SysUser> loadActiveAssignedUsers(Map<Long, List<TeacherClassRel>> relsByClassId) {
        Set<Long> assignedUserIds = relsByClassId.values().stream()
                .flatMap(List::stream)
                .map(TeacherClassRel::getTeacherId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (assignedUserIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, assignedUserIds)
                        .eq(SysUser::getIsDeleted, (byte) 0))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));
    }

    @Override
    @Transactional
    public void assignTeacher(Long classId, Long teacherId) {
        SysClass sysClass = baseMapper.selectById(classId);
        if (sysClass == null) {
            throw new BusinessException(404, "班级不存在");
        }
        
        SysUser teacher = sysUserMapper.selectById(teacherId);
        if (teacher == null) {
            throw new BusinessException(404, "人员不存在");
        }
        
        if (!"TEACHER".equals(teacher.getRole()) && !"ASSISTANT".equals(teacher.getRole())) {
            throw new BusinessException(400, "只能分配讲师或班主任角色");
        }
        
        LambdaQueryWrapper<TeacherClassRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClassRel::getClassId, classId)
               .eq(TeacherClassRel::getTeacherId, teacherId);
        if (teacherClassRelMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "该人员已在班级中");
        }
        
        TeacherClassRel rel = new TeacherClassRel();
        rel.setClassId(classId);
        rel.setTeacherId(teacherId);
        teacherClassRelMapper.insert(rel);
    }

    @Override
    @Transactional
    public void removeTeacher(Long classId, Long teacherId) {
        LambdaQueryWrapper<TeacherClassRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClassRel::getClassId, classId)
               .eq(TeacherClassRel::getTeacherId, teacherId);
        teacherClassRelMapper.delete(wrapper);
    }
}
