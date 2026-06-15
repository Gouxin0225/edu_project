package com.example.edubackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamAnswerDetailExportRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("答卷状态")
    private String status;

    @ExcelProperty("总得分")
    private BigDecimal totalScore;

    @ExcelProperty("交卷时间")
    private String submitTime;

    @ExcelProperty("题号")
    private Integer questionNo;

    @ExcelProperty("题型")
    private String questionType;

    @ExcelProperty("题目内容")
    private String questionContent;

    @ExcelProperty("学生答案")
    private String studentAnswer;

    @ExcelProperty("标准答案")
    private String standardAnswer;

    @ExcelProperty("题目满分")
    private Integer questionScore;

    @ExcelProperty("本题得分")
    private BigDecimal scoreGained;

    @ExcelProperty("判定")
    private String result;
}
