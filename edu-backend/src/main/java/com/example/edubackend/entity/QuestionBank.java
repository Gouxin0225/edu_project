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
 * 智能题库表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("question_bank")
public class QuestionBank implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 课程方向
     */
    @TableField("course_category")
    private String courseCategory;

    /**
     * 知识点
     */
    @TableField("knowledge_point")
    private String knowledgePoint;

    /**
     * 题型: SINGLE, MULTIPLE, JUDGE, SHORT, CODE
     */
    @TableField("type")
    private String type;

    /**
     * 难度: EASY, MEDIUM, HARD
     */
    @TableField("difficulty")
    private String difficulty;

    /**
     * 题干内容
     */
    @TableField("content")
    private String content;

    /**
     * 客观题选项
     */
    @TableField("options_json")
    private String optionsJson;

    /**
     * 标准答案
     */
    @TableField("standard_answer")
    private String standardAnswer;

    /**
     * 详细解析
     */
    @TableField("analysis")
    private String analysis;

    /**
     * 出题人ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 是否由AI生成
     */
    @TableField("is_ai_generated")
    private Byte isAiGenerated;

    /**
     * 是否公开: 0-私有, 1-公开
     */
    @TableField("is_public")
    private Byte isPublic;

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
