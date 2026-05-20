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
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("document_id")
    private Long documentId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("course_category")
    private String courseCategory;

    @TableField("knowledge_point")
    private String knowledgePoint;

    @TableField("content")
    private String content;

    @TableField("source_title")
    private String sourceTitle;

    @TableField("source_name")
    private String sourceName;

    @TableField("visibility")
    private String visibility;

    @TableField("access_scope")
    private String accessScope;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("owner_role")
    private String ownerRole;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
