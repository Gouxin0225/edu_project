package com.example.edubackend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateUserDTO;
import com.example.edubackend.dto.CreateUserRespDTO;
import com.example.edubackend.dto.ImportUserDTO;
import com.example.edubackend.dto.PasswordResetRespDTO;
import com.example.edubackend.dto.StudentClassHistoryVO;
import com.example.edubackend.dto.StudentInfoVO;
import com.example.edubackend.dto.UpdateStudentDTO;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.AuthTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherStudentController {
    private static final String DEFAULT_INITIAL_PASSWORD = "openlab@123";
    private static final String PHONE_PATTERN = "^1\\d{10}$";

    private final SysUserMapper sysUserMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysClassMapper sysClassMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    @PostMapping("/student")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<CreateUserRespDTO> createStudent(@Valid @RequestBody CreateUserDTO dto) {
        if (!"STUDENT".equals(dto.getRole())) {
            throw new BusinessException(400, "只能创建学生账号");
        }

        if (dto.getClassId() == null) {
            throw new BusinessException(400, "必须指定班级");
        }

        Set<Long> allowedClassIds = getAllowedClassIds();
        if (!allowedClassIds.contains(dto.getClassId())) {
            throw new BusinessException(403, "无权在此班级创建学生");
        }

        SysUser existing = findExistingStudent(dto.getPhone(), dto.getUsername());
        if (existing != null) {
            ensureExistingStudentMatches(existing, dto.getRealName());
            ensureStudentInClass(existing, dto.getClassId(), "ASSISTANT_ADD", UserContext.getUserId());
            existing.setPassword(null);
            CreateUserRespDTO resp = new CreateUserRespDTO();
            resp.setId(existing.getId());
            resp.setUsername(existing.getUsername());
            resp.setRealName(existing.getRealName());
            resp.setRole(existing.getRole());
            resp.setInitialPassword(null);
            return Result.success(resp);
        }

        String username = dto.getUsername();
        if (!StringUtils.hasText(username) && StringUtils.hasText(dto.getPhone())) {
            String phone = dto.getPhone().trim();
            if (phone.length() >= 6) {
                username = "op" + phone.substring(phone.length() - 6);
            } else {
                username = "op" + phone;
            }
        }

        if (!StringUtils.hasText(username)) {
            throw new BusinessException(400, "用户名不能为空");
        }

        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getIsDeleted, (byte) 0)) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (StringUtils.hasText(dto.getPhone()) && sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, dto.getPhone().trim())
                .eq(SysUser::getIsDeleted, (byte) 0)) > 0) {
            throw new BusinessException(400, "手机号已存在");
        }

        String initialPassword = DEFAULT_INITIAL_PASSWORD;

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRealName(dto.getRealName());
        user.setRole("STUDENT");
        user.setPhone(textOrNull(dto.getPhone()));
        user.setSchoolName(textOrNull(dto.getSchoolName()));
        user.setCreateSource("ASSISTANT_ADD");
        user.setClassId(dto.getClassId());
        user.setCreatedBy(UserContext.getUserId());
        user.setMustChangePassword((byte) 1);

        sysUserMapper.insert(user);
        ensureStudentInClass(user, dto.getClassId(), "ASSISTANT_ADD", UserContext.getUserId());

        log.info("{}创建学生: {}, 班级: {}", UserContext.getUserId(), username, dto.getClassId());

        CreateUserRespDTO resp = new CreateUserRespDTO();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        resp.setInitialPassword(initialPassword);
        return Result.success(resp);
    }

    @PostMapping("/student/batch")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Map<String, Object>> batchImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("classId") Long classId) throws IOException {
        
        Set<Long> allowedClassIds = getAllowedClassIds();
        if (!allowedClassIds.contains(classId)) {
            throw new BusinessException(403, "无权在此班级创建学生");
        }

        List<ImportUserDTO> students = EasyExcel.read(file.getInputStream())
                .head(ImportUserDTO.class)
                .sheet()
                .doReadSync();

        List<String> errors = new ArrayList<>();
        List<SysUser> successStudents = new ArrayList<>();
        List<Map<String, String>> credentials = new ArrayList<>();
        int created = 0;
        int reused = 0;
        int skipped = 0;

        for (int i = 0; i < students.size(); i++) {
            ImportUserDTO dto = students.get(i);
            int rowNum = i + 2;
            
            try {
                if (!isImportRowPresent(dto)) {
                    continue;
                }
                if (!"STUDENT".equals(dto.getRole()) && StringUtils.hasText(dto.getRole())) {
                    errors.add("第" + rowNum + "行: 角色必须是STUDENT");
                    continue;
                }

                String phone = textOrNull(dto.getPhone());
                String realName = textOrNull(dto.getRealName());
                if (!StringUtils.hasText(phone)) {
                    errors.add("第" + rowNum + "行: 手机号不能为空，且手机号必须作为登录账号");
                    continue;
                }
                if (!phone.matches(PHONE_PATTERN)) {
                    errors.add("第" + rowNum + "行: 手机号格式不正确，必须为11位手机号");
                    continue;
                }
                if (!StringUtils.hasText(realName)) {
                    errors.add("第" + rowNum + "行: 真实姓名不能为空");
                    continue;
                }
                String username = phone;

                SysUser existing = findExistingStudent(phone, username);
                if (existing != null) {
                    ensureExistingStudentMatches(existing, realName);
                    if (isStudentInClass(existing.getId(), classId)) {
                        skipped++;
                    } else {
                        ensureStudentInClass(existing, classId, "ASSISTANT_IMPORT", UserContext.getUserId());
                        successStudents.add(existing);
                        reused++;
                    }
                    continue;
                }

                if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getIsDeleted, (byte) 0)) > 0) {
                    errors.add("第" + rowNum + "行: 用户名" + username + "已存在");
                    continue;
                }
                if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone)
                        .eq(SysUser::getIsDeleted, (byte) 0)) > 0) {
                    errors.add("第" + rowNum + "行: 手机号" + phone + "已存在");
                    continue;
                }

                String initialPassword = DEFAULT_INITIAL_PASSWORD;

                SysUser user = new SysUser();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(initialPassword));
                user.setRealName(realName);
                user.setRole("STUDENT");
                user.setPhone(phone);
                user.setSchoolName(textOrNull(dto.getSchoolName()));
                user.setCreateSource("ASSISTANT_IMPORT");
                user.setClassId(classId);
                user.setCreatedBy(UserContext.getUserId());
                user.setMustChangePassword((byte) 1);

                sysUserMapper.insert(user);
                ensureStudentInClass(user, classId, "ASSISTANT_IMPORT", UserContext.getUserId());
                successStudents.add(user);
                created++;
                credentials.add(Map.of(
                        "username", user.getUsername(),
                        "realName", user.getRealName(),
                        "initialPassword", initialPassword
                ));
            } catch (Exception e) {
                errors.add("第" + rowNum + "行: " + e.getMessage());
            }
        }

        log.info("{}批量导入学生: 班级:{}, 成功{}条, 失败{}条", 
                UserContext.getUserId(), classId, successStudents.size(), errors.size());
        
        return Result.success(Map.of(
                "success", successStudents.size(),
                "failed", errors.size(),
                "created", created,
                "reused", reused,
                "skipped", skipped,
                "errors", errors,
                "credentials", credentials
        ));
    }

    private boolean isImportRowPresent(ImportUserDTO dto) {
        return dto != null && (StringUtils.hasText(dto.getUsername())
                || StringUtils.hasText(dto.getRealName())
                || StringUtils.hasText(dto.getPhone())
                || StringUtils.hasText(dto.getSchoolName())
                || StringUtils.hasText(dto.getRole())
                || dto.getClassId() != null);
    }

    private Set<Long> getAllowedClassIds() {
        Long userId = UserContext.getUserId();
        String userRole = UserContext.getUser().getRole();
        
        if ("ADMIN".equals(userRole)) {
            LambdaQueryWrapper<SysClass> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysClass::getStatus, (byte) 1);
            return sysClassMapper.selectList(wrapper).stream()
                    .map(SysClass::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
        }
        
        LambdaQueryWrapper<TeacherClassRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClassRel::getTeacherId, userId);
        return teacherClassRelMapper.selectList(wrapper).stream()
                .map(TeacherClassRel::getClassId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    @GetMapping("/students/search-existing")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<StudentInfoVO>> searchExistingStudents(@RequestParam Long targetClassId,
                                                              @RequestParam String keyword,
                                                              @RequestParam(defaultValue = "20") Integer limit) {
        Set<Long> allowedClassIds = getAllowedClassIds();
        if (!allowedClassIds.contains(targetClassId)) {
            throw new BusinessException(403, "无权在此班级添加学生");
        }

        String searchKeyword = textOrNull(keyword);
        if (!StringUtils.hasText(searchKeyword)) {
            return Result.success(List.of());
        }

        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0)
                .and(w -> w.like(SysUser::getUsername, searchKeyword)
                        .or()
                        .like(SysUser::getRealName, searchKeyword)
                        .or()
                        .like(SysUser::getPhone, searchKeyword)
                        .or()
                        .like(SysUser::getSchoolName, searchKeyword))
                .orderByDesc(SysUser::getCreateTime)
                .last("LIMIT " + safeLimit);

        List<SysUser> students = sysUserMapper.selectList(wrapper);
        return Result.success(students.stream().map(this::toStudentInfoVO).toList());
    }

    @PutMapping("/student/{id}/reset-password")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<PasswordResetRespDTO> resetStudentPassword(@PathVariable Long id) {
        SysUser student = sysUserMapper.selectById(id);
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(404, "学生不存在");
        }

        Set<Long> allowedClassIds = getAllowedClassIds();
        if (!studentInAnyAllowedClass(student.getId(), allowedClassIds)) {
            throw new BusinessException(403, "无权重置该学生密码");
        }

        String newPassword = DEFAULT_INITIAL_PASSWORD;
        student.setPassword(passwordEncoder.encode(newPassword));
        student.setMustChangePassword((byte) 1);
        sysUserMapper.updateById(student);
        authTokenService.invalidateUserToken(student.getId());

        log.info("{}重置学生密码: {}", UserContext.getUserId(), student.getUsername());

        PasswordResetRespDTO resp = new PasswordResetRespDTO();
        resp.setUserId(student.getId());
        resp.setUsername(student.getUsername());
        resp.setNewPassword(newPassword);
        resp.setMessage("密码已重置为 " + DEFAULT_INITIAL_PASSWORD);
        return Result.success(resp);
    }

    @PutMapping("/student/{id}")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<StudentInfoVO> updateStudent(@PathVariable Long id, @Valid @RequestBody UpdateStudentDTO dto) {
        SysUser student = sysUserMapper.selectById(id);
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(404, "学生不存在");
        }
        ensureCanManageStudent(student);

        String username = dto.getUsername().trim();
        String realName = dto.getRealName().trim();
        String phone = textOrNull(dto.getPhone());

        if (StringUtils.hasText(phone) && !phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(400, "手机号格式不正确，必须为11位手机号");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getIsDeleted, (byte) 0)
                .ne(SysUser::getId, id)) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (StringUtils.hasText(phone) && sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .eq(SysUser::getIsDeleted, (byte) 0)
                .ne(SysUser::getId, id)) > 0) {
            throw new BusinessException(400, "手机号已存在");
        }

        boolean usernameChanged = !username.equals(student.getUsername());
        student.setUsername(username);
        student.setRealName(realName);
        student.setPhone(phone);
        student.setSchoolName(textOrNull(dto.getSchoolName()));
        sysUserMapper.updateById(student);
        if (usernameChanged) {
            authTokenService.invalidateUserToken(student.getId());
        }

        log.info("{}修改学生资料: {}", UserContext.getUserId(), username);
        return Result.success(toStudentInfoVO(student));
    }

    @DeleteMapping("/student/{id}/class/{classId}")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Void> removeStudentFromClass(@PathVariable Long id, @PathVariable Long classId) {
        SysUser student = sysUserMapper.selectById(id);
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(404, "学生不存在");
        }
        Set<Long> allowedClassIds = getAllowedClassIds();
        if (!allowedClassIds.contains(classId)) {
            throw new BusinessException(403, "无权操作该班级");
        }

        List<ClassStudentRel> activeRels = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getStudentId, id)
                .eq(ClassStudentRel::getClassId, classId)
                .eq(ClassStudentRel::getStatus, "ACTIVE"));
        if (activeRels.isEmpty() && (student.getClassId() == null || !student.getClassId().equals(classId))) {
            throw new BusinessException(404, "该学生不在当前班级");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ClassStudentRel rel : activeRels) {
            rel.setStatus("LEFT");
            rel.setLeaveTime(now);
            classStudentRelMapper.updateById(rel);
        }
        if (student.getClassId() != null && student.getClassId().equals(classId)) {
            Long nextClassId = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                            .eq(ClassStudentRel::getStudentId, id)
                            .eq(ClassStudentRel::getStatus, "ACTIVE")
                            .ne(ClassStudentRel::getClassId, classId)
                            .orderByDesc(ClassStudentRel::getJoinTime))
                    .stream()
                    .map(ClassStudentRel::getClassId)
                    .filter(nextId -> nextId != null)
                    .findFirst()
                    .orElse(null);
            student.setClassId(nextClassId);
            sysUserMapper.updateById(student);
        }

        log.info("{}将学生移出班级: student={}, class={}", UserContext.getUserId(), student.getUsername(), classId);
        return Result.success();
    }

    @GetMapping("/students")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<StudentInfoVO>> getStudents(@RequestParam(required = false) Long classId,
                                                   @RequestParam(required = false) String keyword) {
        String role = UserContext.getUser().getRole();
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<SysUser> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0);

        // 如果是讲师，且没传 classId，则只看自己带的班
        // 如果是管理员或班主任，没传 classId 则看全部
        if ("TEACHER".equals(role)) {
            Set<Long> myClassIds = teacherClassRelMapper.selectList(new LambdaQueryWrapper<TeacherClassRel>()
                    .eq(TeacherClassRel::getTeacherId, userId))
                    .stream().map(TeacherClassRel::getClassId).collect(Collectors.toSet());
            
            if (classId != null) {
                if (!myClassIds.contains(classId)) {
                    throw new BusinessException(403, "无权查看该班级学员");
                }
                List<Long> studentIds = getVisibleStudentIdsByClass(classId);
                if (studentIds.isEmpty()) return Result.success(List.of());
                studentWrapper.in(SysUser::getId, studentIds);
            } else {
                if (myClassIds.isEmpty()) return Result.success(List.of());
                List<Long> studentIds = getVisibleStudentIdsByClasses(myClassIds);
                if (studentIds.isEmpty()) return Result.success(List.of());
                studentWrapper.in(SysUser::getId, studentIds);
            }
        } else if (classId != null) {
            // ADMIN 或 ASSISTANT 指定了班级
            List<Long> studentIds = getVisibleStudentIdsByClass(classId);
            if (studentIds.isEmpty()) return Result.success(List.of());
            studentWrapper.in(SysUser::getId, studentIds);
        }

        if (StringUtils.hasText(keyword)) {
            String searchKeyword = keyword.trim();
            studentWrapper.and(w -> w.like(SysUser::getUsername, searchKeyword)
                    .or()
                    .like(SysUser::getRealName, searchKeyword)
                    .or()
                    .like(SysUser::getPhone, searchKeyword)
                    .or()
                    .like(SysUser::getSchoolName, searchKeyword));
        }

        List<SysUser> students = sysUserMapper.selectList(studentWrapper.orderByDesc(SysUser::getCreateTime));
        List<StudentInfoVO> result = new ArrayList<>();

        for (SysUser s : students) {
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

            // 获取班级历史
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
            result.add(vo);
        }

        return Result.success(result);
    }

    private StudentInfoVO toStudentInfoVO(SysUser s) {
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
        return vo;
    }

    private void ensureCanManageStudent(SysUser student) {
        String role = UserContext.getUser().getRole();
        if ("ADMIN".equals(role)) {
            return;
        }
        Set<Long> allowedClassIds = getAllowedClassIds();
        if (!studentInAnyAllowedClass(student.getId(), allowedClassIds)) {
            throw new BusinessException(403, "无权操作该学生");
        }
    }

    private List<Long> getStudentIdsByClass(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getClassId, classId)
                .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getStudentId)
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private List<Long> getStudentIdsByClasses(Set<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        Set<Long> safeClassIds = classIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (safeClassIds.isEmpty()) {
            return List.of();
        }
        return classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                .in(ClassStudentRel::getClassId, safeClassIds)
                .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getStudentId)
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private List<Long> getVisibleStudentIdsByClass(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return getVisibleStudentIdsByClasses(Set.of(classId));
    }

    private List<Long> getVisibleStudentIdsByClasses(Set<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        Set<Long> safeClassIds = classIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (safeClassIds.isEmpty()) {
            return List.of();
        }

        Set<Long> visibleIds = new LinkedHashSet<>(getStudentIdsByClasses(safeClassIds));

        List<SysUser> legacyClassStudents = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0)
                .in(SysUser::getClassId, safeClassIds));
        List<Long> legacyCandidateIds = legacyClassStudents.stream()
                .map(SysUser::getId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (!legacyCandidateIds.isEmpty()) {
            Set<Long> studentsWithAnyClassRel = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                            .in(ClassStudentRel::getStudentId, legacyCandidateIds))
                    .stream()
                    .map(ClassStudentRel::getStudentId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            for (Long candidateId : legacyCandidateIds) {
                if (!studentsWithAnyClassRel.contains(candidateId)) {
                    visibleIds.add(candidateId);
                }
            }
        }

        return new ArrayList<>(visibleIds);
    }

    private SysUser findExistingStudent(String phone, String username) {
        if (!StringUtils.hasText(phone) && !StringUtils.hasText(username)) {
            return null;
        }
        List<SysUser> matches = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0)
                .and(w -> {
                    boolean hasPhone = StringUtils.hasText(phone);
                    boolean hasUsername = StringUtils.hasText(username);
                    if (hasPhone) {
                        w.eq(SysUser::getPhone, phone.trim());
                    }
                    if (hasPhone && hasUsername) {
                        w.or();
                    }
                    if (hasUsername) {
                        w.eq(SysUser::getUsername, username.trim());
                    }
                }));
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            throw new BusinessException(400, "手机号和用户名匹配到不同学生账号，请核对后再添加");
        }
        return matches.get(0);
    }

    private void ensureExistingStudentMatches(SysUser existing, String realName) {
        String inputName = textOrNull(realName);
        String existingName = textOrNull(existing.getRealName());
        if (StringUtils.hasText(inputName) && StringUtils.hasText(existingName)
                && !inputName.equals(existingName)) {
            throw new BusinessException(400, "手机号或用户名已存在，但姓名不一致：已有账号姓名为"
                    + existingName + "，请核对后再添加");
        }
    }

    private void ensureStudentInClass(SysUser student, Long classId, String source, Long operatorId) {
        if (isStudentInClass(student.getId(), classId)) {
            return;
        }

        ClassStudentRel existingRel = classStudentRelMapper.selectOne(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getStudentId, student.getId())
                .eq(ClassStudentRel::getClassId, classId)
                .last("LIMIT 1"));
        if (existingRel != null) {
            existingRel.setStatus("ACTIVE");
            existingRel.setJoinSource(source);
            existingRel.setCreatedBy(operatorId);
            existingRel.setJoinTime(LocalDateTime.now());
            existingRel.setLeaveTime(null);
            classStudentRelMapper.updateById(existingRel);
            if (student.getClassId() == null) {
                student.setClassId(classId);
                sysUserMapper.updateById(student);
            }
            return;
        }

        ClassStudentRel rel = new ClassStudentRel();
        rel.setClassId(classId);
        rel.setStudentId(student.getId());
        rel.setStatus("ACTIVE");
        rel.setJoinSource(source);
        rel.setCreatedBy(operatorId);
        rel.setJoinTime(LocalDateTime.now());
        classStudentRelMapper.insert(rel);

        if (student.getClassId() == null) {
            student.setClassId(classId);
            sysUserMapper.updateById(student);
        }
    }

    private boolean isStudentInClass(Long studentId, Long classId) {
        return classStudentRelMapper.selectCount(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getStudentId, studentId)
                .eq(ClassStudentRel::getClassId, classId)
                .eq(ClassStudentRel::getStatus, "ACTIVE")) > 0;
    }

    private boolean studentInAnyAllowedClass(Long studentId, Set<Long> allowedClassIds) {
        if (allowedClassIds.isEmpty()) {
            return false;
        }
        SysUser student = sysUserMapper.selectById(studentId);
        if (student != null && student.getClassId() != null && allowedClassIds.contains(student.getClassId())) {
            return true;
        }
        return classStudentRelMapper.selectCount(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getStudentId, studentId)
                .eq(ClassStudentRel::getStatus, "ACTIVE")
                .in(ClassStudentRel::getClassId, allowedClassIds)) > 0;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
