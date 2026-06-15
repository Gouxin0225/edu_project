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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_campus_grade")
public class ScheduleCampusGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("school_name")
    private String schoolName;
    @TableField("grade_name")
    private String gradeName;
    @TableField("class_id")
    private Long classId;
    @TableField("major_direction")
    private String majorDirection;
    @TableField("student_count")
    private Integer studentCount;
    @TableField("start_date")
    private LocalDate startDate;
    @TableField("expected_graduate_date")
    private LocalDate expectedGraduateDate;
    @TableField("status")
    private String status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
