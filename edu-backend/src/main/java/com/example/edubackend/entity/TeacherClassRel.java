package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 讲师-班级多对多关联表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("teacher_class_rel")
public class TeacherClassRel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 讲师用户ID
     */
    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 班级ID
     */
    @TableField("class_id")
    private Long classId;

    /**
     * 关联创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
