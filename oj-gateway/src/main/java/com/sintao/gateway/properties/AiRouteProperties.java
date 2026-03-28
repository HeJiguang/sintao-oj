package com.sintao.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "ai.gateway")
public class AiRouteProperties {

    private Boolean routeEnabled = true;

    private String uri = "lb://oj-agent";

    private String publicPrefix = "/ai";

    private String targetPrefix = "/api";

    public Boolean getRouteEnabled() {
        return routeEnabled;
    }

    public void setRouteEnabled(Boolean routeEnabled) {
        this.routeEnabled = routeEnabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPublicPrefix() {
        return publicPrefix;
    }

    public void setPublicPrefix(String publicPrefix) {
        this.publicPrefix = publicPrefix;
    }

    public String getTargetPrefix() {
        return targetPrefix;
    }

    public void setTargetPrefix(String targetPrefix) {
        this.targetPrefix = targetPrefix;
    }
}
