package com.example.edubackend.controller;

import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.PasswordChangeDTO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.AuthTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }

        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(encodedPassword);
        updateUser.setMustChangePassword((byte) 0);
        sysUserMapper.updateById(updateUser);
        authTokenService.invalidateUserToken(userId);

        return Result.success("密码修改成功，请重新登录");
    }
}
