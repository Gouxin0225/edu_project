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
 * 问卷调查任务主表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("survey_task")
public class SurveyTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问卷ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 问卷标题
     */
    @TableField("title")
    private String title;

    /**
     * 截止填写时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 发布人ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 目标班级ID集合
     */
    @TableField("target_class_ids")
    private String targetClassIds;

    /**
     * 是否强制匿名
     */
    @TableField("is_anonymous_required")
    private Byte isAnonymousRequired;

    /**
     * 被评价讲师ID
     */
    @TableField("target_teacher_id")
    private Long targetTeacherId;

    /**
     * 状态: 0-草稿, 1-已发布
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 软删除标记
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
