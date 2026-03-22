package com.sintao.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.ignore")
//从nacos读取所有前缀为：security.ignore
public class IgnoreWhiteProperties {
    /**
     * 放行白名单配置，网关不校验此处的白名�?     */
    private List<String> whites = new ArrayList<>();


    public List<String> getWhites() {
        return whites;
    }

    public void setWhites(List<String> whites) {
        this.whites = whites;
    }
}

