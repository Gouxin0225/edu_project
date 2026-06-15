package com.example.edubackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.entity.OperationAuditLog;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.OperationAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationAuditLogService {

    private final OperationAuditLogMapper auditLogMapper;

    public void record(String action, String targetType, Long targetId, String detail) {
        try {
            SysUser operator = UserContext.getUser();
            OperationAuditLog logEntry = new OperationAuditLog();
            if (operator != null) {
                logEntry.setOperatorId(operator.getId());
                logEntry.setOperatorUsername(operator.getUsername());
                logEntry.setOperatorRealName(operator.getRealName());
                logEntry.setOperatorRole(operator.getRole());
            }
            logEntry.setAction(action);
            logEntry.setTargetType(targetType);
            logEntry.setTargetId(targetId);
            logEntry.setDetail(detail);
            logEntry.setClientIp(resolveClientIp());
            logEntry.setCreateTime(LocalDateTime.now());
            auditLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("写入操作审计日志失败: {}", e.getMessage());
        }
    }

    public IPage<OperationAuditLog> page(Integer page, Integer size, String action, String keyword) {
        Page<OperationAuditLog> pageParam = new Page<>(page == null || page < 1 ? 1 : page, size == null || size < 1 ? 20 : size);
        LambdaQueryWrapper<OperationAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationAuditLog::getAction, action.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(OperationAuditLog::getOperatorUsername, value)
                    .or()
                    .like(OperationAuditLog::getOperatorRealName, value)
                    .or()
                    .like(OperationAuditLog::getDetail, value));
        }
        wrapper.orderByDesc(OperationAuditLog::getCreateTime);
        return auditLogMapper.selectPage(pageParam, wrapper);
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
