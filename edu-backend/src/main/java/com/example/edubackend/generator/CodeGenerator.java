package com.example.edubackend.generator;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String moduleName = "src/main/java/com/example/edubackend";
        String outputDir = projectPath + "/" + moduleName;
        String tablePrefix = "";

        FastAutoGenerator.create("jdbc:mysql://localhost:3306/edu_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false",
                        "root", "")
                .globalConfig(builder -> {
                    builder.author("auto-generator")
                            .outputDir(outputDir)
                            .dateType(DateType.TIME_PACK)
                            .commentDate("yyyy-MM-dd")
                            .disableOpenDir();
                })
                .packageConfig(builder -> {
                    builder.parent("com.example.edubackend")
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    builder.addInclude(getTables())
                            .addTablePrefix(tablePrefix)
                            .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .idType(IdType.AUTO)
                            .logicDeleteColumnName("is_deleted")
                            .logicDeletePropertyName("deleted")
                            .addTableFills(
                                    new Column("create_time", FieldFill.INSERT),
                                    new Column("update_time", FieldFill.INSERT_UPDATE)
                            )
                            .mapperBuilder()
                            .enableMapperAnnotation()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .serviceBuilder()
                            .formatServiceFileName("I%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                            .controllerBuilder()
                            .enableRestStyle()
                            .enableHyphenStyle()
                            .formatFileName("%sController");
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }

    protected static String[] getTables() {
        return new String[]{
                "sys_class",
                "sys_user",
                "teacher_class_rel",
                "question_bank",
                "assessment_task",
                "task_question_rel",
                "student_submission",
                "student_answer_detail",
                "student_mistake_book",
                "survey_task",
                "survey_question",
                "survey_record",
                "survey_answer_detail"
        };
    }
}
