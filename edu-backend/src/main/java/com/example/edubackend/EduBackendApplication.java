package com.example.edubackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.edubackend.mapper")
public class EduBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduBackendApplication.class, args);
    }
}
