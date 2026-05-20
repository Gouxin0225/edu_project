package com.example.edubackend.aspect;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.result.ResultCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@Order(1)
public class RoleCheckAspect {

    @Around("@annotation(requireRole) || @within(requireRole)")
    public Object checkRole(ProceedingJoinPoint point, RequireRole requireRole) throws Throwable {
        if (requireRole == null) {
            requireRole = point.getTarget().getClass().getAnnotation(RequireRole.class);
        }
        
        if (requireRole == null) {
            return point.proceed();
        }

        String userRole = getUserRole();
        if (userRole == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        List<String> allowedRoles = Arrays.asList(requireRole.value());
        if (!allowedRoles.contains(userRole)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        return point.proceed();
    }

    private String getUserRole() {
        return UserContext.getUser() != null ? UserContext.getUser().getRole() : null;
    }
}
