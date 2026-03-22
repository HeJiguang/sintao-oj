package com.sintao.friend;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.sintao.**.mapper")
@EnableFeignClients(basePackages = "com.sintao.api")
public class OjFriendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjFriendApplication.class, args);
    }
}

