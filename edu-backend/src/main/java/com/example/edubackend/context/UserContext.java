package com.example.edubackend.context;

import com.example.edubackend.entity.SysUser;

public class UserContext {

    private static final ThreadLocal<SysUser> userThreadLocal = new ThreadLocal<>();

    public static void setUser(SysUser user) {
        userThreadLocal.set(user);
    }

    public static SysUser getUser() {
        return userThreadLocal.get();
    }

    public static Long getUserId() {
        SysUser user = userThreadLocal.get();
        return user != null ? user.getId() : null;
    }

    public static void remove() {
        userThreadLocal.remove();
    }
}
