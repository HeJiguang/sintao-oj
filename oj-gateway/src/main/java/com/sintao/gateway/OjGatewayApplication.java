package com.sintao.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class OjGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjGatewayApplication.class, args);
    }
}
