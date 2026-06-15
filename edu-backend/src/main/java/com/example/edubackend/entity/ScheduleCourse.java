package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_course")
public class ScheduleCourse implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("course_code")
    private String courseCode;
    @TableField("course_name")
    private String courseName;
    @TableField("course_direction")
    private String courseDirection;
    @TableField("course_level")
    private String courseLevel;
    @TableField("course_type")
    private String courseType;
    @TableField("teacher_capacity")
    private Integer teacherCapacity;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("status")
    private String status;
    @TableField("remark")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
