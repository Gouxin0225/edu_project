package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.entity.OperationAuditLog;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.OperationAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final OperationAuditLogService auditLogService;

    @GetMapping
    @RequireRole("ADMIN")
    public Result<IPage<OperationAuditLog>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        return Result.success(auditLogService.page(page, size, action, keyword));
    }
}
