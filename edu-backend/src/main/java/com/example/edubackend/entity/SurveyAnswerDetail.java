package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 问卷反馈明细表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("survey_answer_detail")
public class SurveyAnswerDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 反馈记录ID
     */
    @TableField("record_id")
    private Long recordId;

    /**
     * 问卷题目ID
     */
    @TableField("survey_question_id")
    private Long surveyQuestionId;

    /**
     * 具体打分或意见
     */
    @TableField("answer_value")
    private String answerValue;
}
