package com.example.edubackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ImportQuestionDTO {
    @ExcelProperty("课程方向")
    private String courseCategory;

    @ExcelProperty("知识点")
    private String knowledgePoint;

    @ExcelProperty("题型")
    private String type;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("题干")
    private String content;

    @ExcelProperty("题干内容")
    private String legacyContent;

    @ExcelProperty("选项A")
    private String optionA;

    @ExcelProperty("选项B")
    private String optionB;

    @ExcelProperty("选项C")
    private String optionC;

    @ExcelProperty("选项D")
    private String optionD;

    @ExcelProperty("选项JSON")
    private String optionsJson;

    @ExcelProperty("正确答案")
    private String standardAnswer;

    @ExcelProperty("标准答案")
    private String legacyStandardAnswer;

    @ExcelProperty("解析")
    private String analysis;
}
