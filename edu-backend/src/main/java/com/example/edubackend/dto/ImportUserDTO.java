package com.example.edubackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportUserDTO {
    @ExcelProperty("用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ExcelProperty("真实姓名")
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("所属学校")
    private String schoolName;

    @ExcelProperty("角色")
    @NotBlank(message = "角色不能为空")
    private String role;

    @ExcelProperty("班级ID")
    private Long classId;
}
