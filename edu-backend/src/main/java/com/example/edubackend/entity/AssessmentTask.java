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
 * 评估任务主表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("assessment_task")
public class AssessmentTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务标题
     */
    @TableField("title")
    private String title;

    /**
     * 任务类型: EXAM, HOMEWORK
     */
    @TableField("type")
    private String type;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 截止时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 试卷总分
     */
    @TableField("total_score")
    private Integer totalScore;

    /**
     * 发布讲师ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 目标班级ID集合
     */
    @TableField("target_class_ids")
    private String targetClassIds;

    /**
     * 高级设置
     */
    @TableField("setting_json")
    private String settingJson;

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
