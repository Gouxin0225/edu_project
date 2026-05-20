package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名/学号(登录用)
     */
    @TableField("username")
    private String username;

    /**
     * BCrypt密码哈希
     */
    @TableField("password")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 角色: ADMIN, TEACHER, ASSISTANT, STUDENT
     */
    @TableField("role")
    private String role;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 所属学校
     */
    @TableField("school_name")
    private String schoolName;

    /**
     * 账号来源
     */
    @TableField("create_source")
    private String createSource;

    /**
     * 所属班级ID(仅学生使用)
     */
    @TableField("class_id")
    private Long classId;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 是否必须修改密码: 1-必须修改, 0-不需要
     */
    @TableField("must_change_password")
    private Byte mustChangePassword;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 软删除标记
     */
    @TableField("is_deleted")
    private Byte isDeleted;
}
