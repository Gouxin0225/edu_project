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
 * 问卷意见反馈记录表
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Getter
@Setter
@TableName("survey_record")
public class SurveyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 反馈记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 问卷ID
     */
    @TableField("survey_id")
    private Long surveyId;

    /**
     * 学生ID
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 提交者IP
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 提交时间
     */
    @TableField("submit_time")
    private LocalDateTime submitTime;
}
