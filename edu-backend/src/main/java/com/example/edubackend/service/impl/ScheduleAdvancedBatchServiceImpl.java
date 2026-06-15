package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.ScheduleAdvancedBatchDTO;
import com.example.edubackend.dto.ScheduleAdvancedBatchVO;
import com.example.edubackend.entity.ScheduleAdvancedBatch;
import com.example.edubackend.entity.ScheduleCourse;
import com.example.edubackend.entity.ScheduleDispatchPlan;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ScheduleAdvancedBatchMapper;
import com.example.edubackend.mapper.ScheduleCourseMapper;
import com.example.edubackend.mapper.ScheduleDispatchPlanMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.OperationAuditLogService;
import com.example.edubackend.service.ScheduleAdvancedBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleAdvancedBatchServiceImpl implements ScheduleAdvancedBatchService {

    private static final Set<String> ADVANCED_COURSE_CODES = Set.of(
            "PENETRATION_TEST", "DEFENSE_PROTECTION", "CLOUD_COMPUTING"
    );
    private static final Set<String> ADVANCED_COURSE_NAMES = Set.of("渗透测试", "防御保护", "云计算");

    private final ScheduleAdvancedBatchMapper batchMapper;
    private final ScheduleCourseMapper courseMapper;
    private final ScheduleDispatchPlanMapper dispatchPlanMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationAuditLogService auditLogService;

    @Override
    public Page<ScheduleAdvancedBatchVO> getBatches(Integer pageNum, Integer pageSize, Long courseId,
                                                    String schoolName, String batchStatus, String keyword) {
        Page<ScheduleAdvancedBatch> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<ScheduleAdvancedBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAdvancedBatch::getIsDeleted, (byte) 0);
        if (courseId != null) {
            wrapper.eq(ScheduleAdvancedBatch::getCourseId, courseId);
        }
        if (StringUtils.hasText(schoolName)) {
            wrapper.eq(ScheduleAdvancedBatch::getSchoolName, schoolName.trim());
        }
        if (StringUtils.hasText(batchStatus)) {
            wrapper.eq(ScheduleAdvancedBatch::getBatchStatus, batchStatus.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(ScheduleAdvancedBatch::getBatchNo, value)
                    .or()
                    .like(ScheduleAdvancedBatch::getBatchName, value)
                    .or()
                    .like(ScheduleAdvancedBatch::getSchoolName, value));
        }
        wrapper.orderByDesc(ScheduleAdvancedBatch::getCreateTime);

        Page<ScheduleAdvancedBatch> result = batchMapper.selectPage(page, wrapper);
        Page<ScheduleAdvancedBatchVO> voPage = new Page<>(page.getCurrent(), page.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(toVOList(result.getRecords()));
        return voPage;
    }

    @Override
    @Transactional
    public ScheduleAdvancedBatchVO createBatch(ScheduleAdvancedBatchDTO dto) {
        ScheduleCourse course = getAdvancedCourse(dto.getCourseId());
        validateDateRange(dto);

        ScheduleAdvancedBatch batch = new ScheduleAdvancedBatch();
        batch.setBatchNo(StringUtils.hasText(dto.getBatchNo()) ? dto.getBatchNo().trim() : generateBatchNo());
        batch.setBatchName(required(dto.getBatchName(), "批次名称不能为空"));
        batch.setCourseId(course.getId());
        batch.setSchoolName(StringUtils.hasText(dto.getSchoolName()) ? dto.getSchoolName().trim() : null);
        batch.setPlanId(dto.getPlanId());
        batch.setBatchStatus(StringUtils.hasText(dto.getBatchStatus()) ? dto.getBatchStatus().trim().toUpperCase() : "DRAFT");
        batch.setExpectedStartDate(dto.getExpectedStartDate());
        batch.setExpectedEndDate(dto.getExpectedEndDate());
        batch.setCapacity(dto.getCapacity());
        batch.setCreatedBy(UserContext.getUserId());
        batch.setIsDeleted((byte) 0);
        batchMapper.insert(batch);

        auditLogService.record("CREATE", "SCHEDULE_ADVANCED_BATCH", batch.getId(),
                "创建高级集中课批次 " + batch.getBatchName() + "，课程 " + course.getCourseName());
        return toVO(batch, course, null, null);
    }

    private ScheduleCourse getAdvancedCourse(Long courseId) {
        if (courseId == null) {
            throw new BusinessException(400, "高级课课程ID不能为空");
        }
        ScheduleCourse course = courseMapper.selectById(courseId);
        if (course == null || Byte.valueOf((byte) 1).equals(course.getIsDeleted())) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!"ACTIVE".equals(course.getStatus())) {
            throw new BusinessException(400, "课程已停用");
        }
        boolean allowed = ADVANCED_COURSE_CODES.contains(course.getCourseCode())
                || ADVANCED_COURSE_NAMES.contains(course.getCourseName());
        if (!allowed) {
            throw new BusinessException(400, "只允许创建渗透测试、防御保护、云计算高级课批次");
        }
        return course;
    }

    private void validateDateRange(ScheduleAdvancedBatchDTO dto) {
        if (dto.getExpectedStartDate() != null && dto.getExpectedEndDate() != null
                && dto.getExpectedEndDate().isBefore(dto.getExpectedStartDate())) {
            throw new BusinessException(400, "预计结束日期不能早于开始日期");
        }
        if (dto.getCapacity() != null && dto.getCapacity() < 0) {
            throw new BusinessException(400, "容量不能小于0");
        }
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private String generateBatchNo() {
        return "AB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private List<ScheduleAdvancedBatchVO> toVOList(List<ScheduleAdvancedBatch> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<Long, ScheduleCourse> courseMap = courseMapper.selectBatchIds(records.stream()
                        .map(ScheduleAdvancedBatch::getCourseId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ScheduleCourse::getId, Function.identity()));
        Map<Long, ScheduleDispatchPlan> planMap = loadPlans(records);
        Map<Long, SysUser> userMap = loadUsers(records);
        return records.stream()
                .map(batch -> toVO(batch, courseMap.get(batch.getCourseId()), planMap.get(batch.getPlanId()),
                        userMap.get(batch.getCreatedBy())))
                .toList();
    }

    private Map<Long, ScheduleDispatchPlan> loadPlans(List<ScheduleAdvancedBatch> records) {
        Set<Long> planIds = records.stream()
                .map(ScheduleAdvancedBatch::getPlanId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (planIds.isEmpty()) {
            return Map.of();
        }
        return dispatchPlanMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(ScheduleDispatchPlan::getId, Function.identity()));
    }

    private Map<Long, SysUser> loadUsers(List<ScheduleAdvancedBatch> records) {
        Set<Long> userIds = records.stream()
                .map(ScheduleAdvancedBatch::getCreatedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private ScheduleAdvancedBatchVO toVO(ScheduleAdvancedBatch batch, ScheduleCourse course,
                                         ScheduleDispatchPlan plan, SysUser createdBy) {
        ScheduleAdvancedBatchVO vo = new ScheduleAdvancedBatchVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setBatchName(batch.getBatchName());
        vo.setCourseId(batch.getCourseId());
        vo.setCourseName(course == null ? null : course.getCourseName());
        vo.setSchoolName(batch.getSchoolName());
        vo.setPlanId(batch.getPlanId());
        vo.setPlanNo(plan == null ? null : plan.getPlanNo());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setExpectedStartDate(batch.getExpectedStartDate());
        vo.setExpectedEndDate(batch.getExpectedEndDate());
        vo.setActualStartDate(batch.getActualStartDate());
        vo.setActualEndDate(batch.getActualEndDate());
        vo.setCapacity(batch.getCapacity());
        vo.setCreatedBy(batch.getCreatedBy());
        vo.setCreatedByName(createdBy == null ? null : createdBy.getRealName());
        vo.setCreateTime(batch.getCreateTime());
        vo.setUpdateTime(batch.getUpdateTime());
        return vo;
    }
}
