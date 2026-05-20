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
 * 问卷动态表单题目表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("survey_question")
public class SurveyQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问卷题目ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属问卷ID
     */
    @TableField("survey_id")
    private Long surveyId;

    /**
     * 题型: STAR, NPS, SCALE, TEXT
     */
    @TableField("type")
    private String type;

    /**
     * 题干
     */
    @TableField("title")
    private String title;

    /**
     * 选项配置
     */
    @TableField("options_json")
    private String optionsJson;

    /**
     * 是否必填
     */
    @TableField("is_required")
    private Byte isRequired;

    /**
     * 题目排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
