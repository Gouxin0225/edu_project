package com.example.edubackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentVisitRecordVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private Long classId;
    private String className;
    private Long visitorId;
    private String visitorName;
    private String visitorRole;
    private String visitMethod;
    private String visitResult;
    private String content;
    private String problemCategory;
    private String conclusion;
    private String attachmentUrl;
    private Boolean resolved;
    private LocalDateTime nextFollowTime;
    private LocalDateTime visitTime;
    private LocalDateTime createTime;
}
