package com.example.edubackend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateUserDTO;
import com.example.edubackend.dto.CreateUserRespDTO;
import com.example.edubackend.dto.ImportUserDTO;
import com.example.edubackend.dto.PasswordResetRespDTO;
import com.example.edubackend.dto.StudentClassHistoryVO;
import com.example.edubackend.dto.StudentInfoVO;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.AuthTokenService;
import com.example.edubackend.service.OperationAuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {
    private static final String DEFAULT_INITIAL_PASSWORD = "openlab@123";

    private final SysUserMapper sysUserMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysClassMapper sysClassMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final OperationAuditLogService auditLogService;

    @PostMapping("/user")
    @RequireRole("ADMIN")
    public Result<CreateUserRespDTO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        if (!"TEACHER".equals(dto.getRole()) && !"ASSISTANT".equals(dto.getRole())) {
            throw new BusinessException(400, "管理员只能创建讲师或班主任账号");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, dto.getUsername());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        String initialPassword = DEFAULT_INITIAL_PASSWORD;
        
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setClassId(null);
        user.setCreatedBy(UserContext.getUserId());
        user.setMustChangePassword((byte) 1);
        
        sysUserMapper.insert(user);
        auditLogService.record("USER_CREATE", "SYS_USER", user.getId(),
                "创建用户 " + user.getUsername() + "，角色 " + user.getRole());
        
        log.info("管理员创建用户: {}, 角色: {}", dto.getUsername(), dto.getRole());
        
        CreateUserRespDTO resp = new CreateUserRespDTO();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        resp.setInitialPassword(initialPassword);
        return Result.success(resp);
    }

    @PostMapping("/user/batch")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> batchImport(@RequestParam("file") MultipartFile file) throws IOException {
        List<ImportUserDTO> users = EasyExcel.read(file.getInputStream())
                .head(ImportUserDTO.class)
                .sheet()
                .doReadSync();

        List<String> errors = new ArrayList<>();
        List<SysUser> successUsers = new ArrayList<>();
        List<Map<String, String>> credentials = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            ImportUserDTO dto = users.get(i);
            int rowNum = i + 2;
            
            try {
                if (!"TEACHER".equals(dto.getRole()) && !"ASSISTANT".equals(dto.getRole())) {
                    errors.add("第" + rowNum + "行: 角色必须是TEACHER或ASSISTANT");
                    continue;
                }

                LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysUser::getUsername, dto.getUsername());
                if (sysUserMapper.selectCount(wrapper) > 0) {
                    errors.add("第" + rowNum + "行: 用户名" + dto.getUsername() + "已存在");
                    continue;
                }

                String initialPassword = DEFAULT_INITIAL_PASSWORD;
                
                SysUser user = new SysUser();
                user.setUsername(dto.getUsername());
                user.setPassword(passwordEncoder.encode(initialPassword));
                user.setRealName(dto.getRealName());
                user.setRole(dto.getRole());
                user.setCreatedBy(UserContext.getUserId());
                user.setMustChangePassword((byte) 1);
                
                sysUserMapper.insert(user);
                auditLogService.record("USER_IMPORT", "SYS_USER", user.getId(),
                        "批量导入用户 " + user.getUsername() + "，角色 " + user.getRole());
                successUsers.add(user);
                credentials.add(Map.of(
                        "username", user.getUsername(),
                        "realName", user.getRealName(),
                        "initialPassword", initialPassword
                ));
            } catch (Exception e) {
                errors.add("第" + rowNum + "行: " + e.getMessage());
            }
        }

        log.info("管理员批量导入用户: 成功{}条, 失败{}条", successUsers.size(), errors.size());
        
        return Result.success(Map.of(
                "success", successUsers.size(),
                "failed", errors.size(),
                "errors", errors,
                "credentials", credentials
        ));
    }

    @PutMapping("/user/{id}/reset-password")
    @RequireRole("ADMIN")
    public Result<PasswordResetRespDTO> resetPassword(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String newPassword = DEFAULT_INITIAL_PASSWORD;
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword((byte) 1);
        sysUserMapper.updateById(user);
        authTokenService.invalidateUserToken(id);
        auditLogService.record("USER_RESET_PASSWORD", "SYS_USER", id,
                "重置用户密码 " + user.getUsername());

        log.info("管理员重置用户密码: {}", user.getUsername());
        
        PasswordResetRespDTO resp = new PasswordResetRespDTO();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNewPassword(newPassword);
        resp.setMessage("密码已重置为 " + DEFAULT_INITIAL_PASSWORD);
        return Result.success(resp);
    }

    @DeleteMapping("/user/{id}")
    @RequireRole("ADMIN")
    public Result<Void> deleteUser(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(400, "不能删除管理员账号");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        sysUserMapper.delete(wrapper);
        authTokenService.invalidateUserToken(id);
        auditLogService.record("USER_DELETE", "SYS_USER", id,
                "删除用户 " + user.getUsername() + "，角色 " + user.getRole());
        log.info("管理员删除用户: {}", user.getUsername());
        return Result.success();
    }

    @GetMapping("/user/list")
    @RequireRole("ADMIN")
    public Result<IPage<StudentInfoVO>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = activeUserQuery();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String exactKeyword = keyword.trim();
            wrapper.and(q -> q.eq(SysUser::getUsername, exactKeyword)
                    .or()
                    .eq(SysUser::getRealName, exactKeyword)
                    .or()
                    .eq(SysUser::getPhone, exactKeyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> userPage = sysUserMapper.selectPage(pageParam, wrapper);

        Page<StudentInfoVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<StudentInfoVO> voList = new ArrayList<>();

        for (SysUser s : userPage.getRecords()) {
            StudentInfoVO vo = new StudentInfoVO();
            vo.setId(s.getId());
            vo.setUsername(s.getUsername());
            vo.setRealName(s.getRealName());
            vo.setPhone(s.getPhone());
            vo.setSchoolName(s.getSchoolName());
            vo.setRole(s.getRole());
            vo.setClassId(s.getClassId());
            vo.setCreateTime(s.getCreateTime());

            if (s.getClassId() != null) {
                SysClass c = sysClassMapper.selectById(s.getClassId());
                if (c != null) vo.setClassName(c.getClassName());
            }

            if ("STUDENT".equals(s.getRole())) {
                List<ClassStudentRel> rels = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, s.getId())
                        .orderByDesc(ClassStudentRel::getJoinTime));

                List<StudentClassHistoryVO> history = new ArrayList<>();
                for (ClassStudentRel rel : rels) {
                    SysClass c = sysClassMapper.selectById(rel.getClassId());
                    if (c != null) {
                        StudentClassHistoryVO h = new StudentClassHistoryVO();
                        h.setClassId(c.getId());
                        h.setClassName(c.getClassName());
                        h.setStatus(String.valueOf(c.getStatus()));
                        h.setJoinStatus(rel.getStatus());
                        h.setJoinTime(rel.getJoinTime());
                        h.setLeaveTime(rel.getLeaveTime());
                        history.add(h);
                    }
                }
                vo.setClassHistory(history);
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @GetMapping("/user/stats")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getUserStats() {
        Long total = sysUserMapper.selectCount(activeUserQuery());
        Long teachers = sysUserMapper.selectCount(activeUserQuery().eq(SysUser::getRole, "TEACHER"));
        Long assistants = sysUserMapper.selectCount(activeUserQuery().eq(SysUser::getRole, "ASSISTANT"));
        Long students = sysUserMapper.selectCount(activeUserQuery().eq(SysUser::getRole, "STUDENT"));
        return Result.success(Map.of("total", total, "teachers", teachers, "assistants", assistants, "students", students));
    }

    private LambdaQueryWrapper<SysUser> activeUserQuery() {
        return new LambdaQueryWrapper<SysUser>().eq(SysUser::getIsDeleted, (byte) 0);
    }

}
