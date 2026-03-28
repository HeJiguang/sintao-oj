package com.sintao.common.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 各微服务可在自己的配置文件中通过 security.ignore.whites 自定义免鉴权路径。
 * 示例：
 * <pre>
 * security:
 *   ignore:
 *     whites:
 *       - /user/login
 *       - /user/sendCode
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "security.ignore")
public class SecurityProperties {

    private List<String> whites = new ArrayList<>();

    public List<String> getWhites() {
        return whites;
    }

    public void setWhites(List<String> whites) {
        this.whites = whites;
    }
}
