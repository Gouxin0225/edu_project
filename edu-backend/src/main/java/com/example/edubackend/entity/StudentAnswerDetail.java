package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 学生答题明细表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("student_answer_detail")
public class StudentAnswerDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 提交主表ID
     */
    @TableField("submission_id")
    private Long submissionId;

    /**
     * 题目ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 学生提交的答案
     */
    @TableField("student_answer")
    private String studentAnswer;

    /**
     * 是否正确: 1-正确, 0-错误, 2-部分正确
     */
    @TableField("is_correct")
    private Byte isCorrect;

    /**
     * 该题实际得分
     */
    @TableField("score_gained")
    private BigDecimal scoreGained;

    /**
     * AI建议得分
     */
    @TableField("ai_suggest_score")
    private BigDecimal aiSuggestScore;

    /**
     * AI建议详情(JSON)
     */
    @TableField("ai_suggest_detail")
    private String aiSuggestDetail;
}
