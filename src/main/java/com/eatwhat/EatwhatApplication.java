package com.eatwhat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@MapperScan("com.eatwhat.mapper")
public class EatwhatApplication {
    public static void main(String[] args) {
        SpringApplication.run(EatwhatApplication.class, args);
        System.out.println("========================================");
        System.out.println("外卖项目启动成功！");
        System.out.println("API地址: http://localhost:8080");
        System.out.println("========================================");
    }
}