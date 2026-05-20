package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("outline_generated_question")
public class OutlineGeneratedQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("document_id")
    private Long documentId;

    @TableField("course_category")
    private String courseCategory;

    @TableField("knowledge_point")
    private String knowledgePoint;

    @TableField("type")
    private String type;

    @TableField("difficulty")
    private String difficulty;

    @TableField("content")
    private String content;

    @TableField("options_json")
    private String optionsJson;

    @TableField("standard_answer")
    private String standardAnswer;

    @TableField("analysis")
    private String analysis;

    @TableField("selected")
    private Byte selected;

    @TableField("question_id")
    private Long questionId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
