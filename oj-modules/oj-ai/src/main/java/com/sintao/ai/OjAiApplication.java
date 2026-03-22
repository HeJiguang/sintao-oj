package com.sintao.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * oj-ai 模块不直接访问数据库，排除数据源自动配置，避免启动时报 DataSource 未配置。
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients(basePackages = "com.sintao.api")
public class OjAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjAiApplication.class, args);
    }
}
