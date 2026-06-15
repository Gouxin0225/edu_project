package com.example.edubackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreExportDTO {

    @ExcelProperty("学号")
    private String studentNo;
    
    @ExcelProperty("姓名")
    private String studentName;
    
    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("答卷状态")
    private String status;

    @ExcelProperty("试卷总分")
    private Integer examTotalScore;
    
    @ExcelProperty("总得分")
    private BigDecimal totalScore;
    
    @ExcelProperty("是否及格")
    private String isPass;
    
    @ExcelProperty("交卷时间")
    private String submitTime;

    @ExcelProperty("切屏次数")
    private Integer switchScreenCount;
}
