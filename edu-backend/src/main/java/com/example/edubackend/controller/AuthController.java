package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.LoginDTO;
import com.example.edubackend.dto.LoginVO;
import com.example.edubackend.dto.StudentRegisterDTO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.AuthTokenService;
import com.example.edubackend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final AuthTokenService authTokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDTO.getUsername())
                .eq(SysUser::getIsDeleted, (byte) 0);
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        authTokenService.replaceActiveToken(user.getId(), token);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setId(user.getId());
        loginVO.setRealName(user.getRealName());
        loginVO.setRole(user.getRole());
        loginVO.setMustChangePassword(user.getMustChangePassword() != null && user.getMustChangePassword() == 1);

        log.info("用户登录成功: {}", user.getUsername());
        return Result.success(loginVO);
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody StudentRegisterDTO dto) {
        String phone = dto.getPhone().trim();
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .eq(SysUser::getIsDeleted, (byte) 0)) > 0) {
            throw new BusinessException(400, "该手机号已注册，请直接登录");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, phone)
                .eq(SysUser::getIsDeleted, (byte) 0)) > 0) {
            throw new BusinessException(400, "该手机号已作为用户名注册，请直接登录");
        }

        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setRealName(dto.getRealName().trim());
        user.setSchoolName(dto.getSchoolName().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("STUDENT");
        user.setClassId(null);
        user.setCreatedBy(null);
        user.setCreateSource("SELF_REGISTER");
        user.setMustChangePassword((byte) 0);
        sysUserMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        authTokenService.replaceActiveToken(user.getId(), token);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setId(user.getId());
        loginVO.setRealName(user.getRealName());
        loginVO.setRole(user.getRole());
        loginVO.setMustChangePassword(false);
        log.info("学生自注册成功: {}", user.getUsername());
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = UserContext.getUserId();
        authTokenService.invalidateUserToken(userId);
        return Result.success();
    }

}
