package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("homework_submission_content")
public class HomeworkSubmissionContent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("submission_id")
    private Long submissionId;

    @TableField("content")
    private String content;

    @TableField("git_link")
    private String gitLink;

    @TableField("file_url")
    private String fileUrl;

    @TableField("create_time")
    private LocalDateTime createTime;
}
