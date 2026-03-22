package com.sintao.system;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sintao.**.mapper")
public class OjSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjSystemApplication.class, args);
    }
}

