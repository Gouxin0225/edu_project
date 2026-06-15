package com.example.edubackend.interceptor;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.AuthTokenService;
import com.example.edubackend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;
    private final SysUserMapper sysUserMapper;

    @Value("${jwt.prefix}")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(tokenPrefix)) {
            sendUnauthorizedResponse(response, "未登录或登录已过期");
            return false;
        }

        String token = authHeader.substring(tokenPrefix.length());
        
        if (authTokenService.isBlacklisted(token)) {
            sendUnauthorizedResponse(response, "Token已失效，请重新登录");
            return false;
        }

        try {
            if (jwtUtil.isTokenExpired(token)) {
                sendUnauthorizedResponse(response, "Token已过期，请重新登录");
                return false;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            if (!authTokenService.isActiveOrRestoreToken(userId, token)) {
                sendUnauthorizedResponse(response, "登录状态已失效，请重新登录");
                return false;
            }

            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || user.getIsDeleted() == null || user.getIsDeleted() != 0
                    || !username.equals(user.getUsername())) {
                sendUnauthorizedResponse(response, "用户不存在或已停用");
                return false;
            }
            UserContext.setUser(user);

            if (isPasswordChangeRequired(user) && !isPasswordChangeAllowed(request)) {
                sendPasswordChangeRequiredResponse(response, "请先修改初始密码");
                return false;
            }

            if (!hasRequiredRole(handler, user.getRole())) {
                sendForbiddenResponse(response, "没有权限访问该资源");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Token无效，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.remove();
    }

    private boolean hasRequiredRole(Object handler, String userRole) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }
        return StringUtils.hasText(userRole) && Arrays.asList(requireRole.value()).contains(userRole);
    }

    private boolean isPasswordChangeRequired(SysUser user) {
        return user.getMustChangePassword() != null && user.getMustChangePassword() == 1;
    }

    private boolean isPasswordChangeAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ("/user/password".equals(path) && "PUT".equalsIgnoreCase(request.getMethod()))
                || ("/auth/logout".equals(path) && "POST".equalsIgnoreCase(request.getMethod()));
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void sendPasswordChangeRequiredResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(428);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 428);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
