package com.example.edubackend.entity;

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
 * 学生智能错题本
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("student_mistake_book")
public class StudentMistakeBook implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错题本记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 错题ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 累计答错次数
     */
    @TableField("wrong_count")
    private Integer wrongCount;

    /**
     * 最近一次答错时间
     */
    @TableField("last_wrong_time")
    private LocalDateTime lastWrongTime;

    /**
     * 是否已攻克: 1-已掌握, 0-待攻克
     */
    @TableField("is_mastered")
    private Byte isMastered;
}
