package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 学生提交记录主表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("student_submission")
public class StudentSubmission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提交记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 学生ID
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 状态: UN, SUBMITTED, GRADED, RETURNED
     */
    @TableField("status")
    private String status;

    /**
     * 开始答题时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 最终总得分
     */
    @TableField("total_score_gained")
    private BigDecimal totalScoreGained;

    /**
     * 讲师总评语
     */
    @TableField("teacher_comment")
    private String teacherComment;

    /**
     * 班主任批注
     */
    @TableField("assistant_comment")
    private String assistantComment;

    /**
     * 实际交卷时间
     */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /**
     * 提交版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 上一版本提交记录ID
     */
    @TableField("parent_submission_id")
    private Long parentSubmissionId;

    /**
     * 考试切屏次数
     */
    @TableField("switch_screen_count")
    private Integer switchScreenCount;
}
