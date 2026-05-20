package com.example.edubackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class QuestionExcelExportRow {
    @ExcelProperty(value = "课程方向", index = 0)
    @ColumnWidth(12)
    private String courseCategory;

    @ExcelProperty(value = "知识点", index = 1)
    @ColumnWidth(18)
    private String knowledgePoint;

    @ExcelProperty(value = "题型", index = 2)
    @ColumnWidth(12)
    private String type;

    @ExcelProperty(value = "难度", index = 3)
    @ColumnWidth(10)
    private String difficulty;

    @ExcelProperty(value = "题干", index = 4)
    @ColumnWidth(40)
    private String content;

    @ExcelProperty(value = "选项A", index = 5)
    @ColumnWidth(24)
    private String optionA;

    @ExcelProperty(value = "选项B", index = 6)
    @ColumnWidth(24)
    private String optionB;

    @ExcelProperty(value = "选项C", index = 7)
    @ColumnWidth(24)
    private String optionC;

    @ExcelProperty(value = "选项D", index = 8)
    @ColumnWidth(24)
    private String optionD;

    @ExcelProperty(value = "正确答案", index = 9)
    @ColumnWidth(16)
    private String standardAnswer;

    @ExcelProperty(value = "解析", index = 10)
    @ColumnWidth(30)
    private String analysis;
}
