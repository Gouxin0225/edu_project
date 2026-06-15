package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_teacher_skill")
public class ScheduleTeacherSkill implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("teacher_id")
    private Long teacherId;
    @TableField("course_id")
    private Long courseId;
    @TableField("skill_level")
    private String skillLevel;
    @TableField("certified")
    private Byte certified;
    @TableField("score")
    private BigDecimal score;
    @TableField("last_teach_time")
    private LocalDateTime lastTeachTime;
    @TableField("status")
    private String status;
    @TableField("remark")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
