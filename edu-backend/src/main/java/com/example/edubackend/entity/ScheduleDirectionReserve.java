package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_direction_reserve")
public class ScheduleDirectionReserve implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("school_name")
    private String schoolName;
    @TableField("direction_code")
    private String directionCode;
    @TableField("course_level")
    private String courseLevel;
    @TableField("min_teacher_count")
    private Integer minTeacherCount;
    @TableField("min_expert_count")
    private Integer minExpertCount;
    @TableField("status")
    private String status;
    @TableField("remark")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
