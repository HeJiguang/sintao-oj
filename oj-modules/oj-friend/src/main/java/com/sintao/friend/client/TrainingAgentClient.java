package com.sintao.friend.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sintao.friend.client.dto.AgentTrainingPlanRequest;
import com.sintao.friend.client.dto.AgentTrainingPlanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TrainingAgentClient {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Value("${training.agent.base-url:}")
    private String baseUrl;

    @Value("${training.agent.service-id:oj-agent}")
    private String serviceId;

    @Value("${training.agent.plan-path:/api/training/plan}")
    private String planPath;

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    public AgentTrainingPlanResponse generatePlan(AgentTrainingPlanRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resolvePlanUrl()))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            OBJECT_MAPPER.writeValueAsString(request),
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Training agent returned HTTP " + response.statusCode() + ": " + response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), AgentTrainingPlanResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call training agent", e);
        }
    }

    private String resolvePlanUrl() {
        String resolvedBaseUrl = resolveBaseUrl();
        if (resolvedBaseUrl.endsWith("/") && planPath.startsWith("/")) {
            return resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1) + planPath;
        }
        if (!resolvedBaseUrl.endsWith("/") && !planPath.startsWith("/")) {
            return resolvedBaseUrl + "/" + planPath;
        }
        return resolvedBaseUrl + planPath;
    }

    private String resolveBaseUrl() {
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl.trim();
        }
        if (discoveryClient == null) {
            throw new IllegalStateException("Training agent base-url is blank and DiscoveryClient is unavailable");
        }
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("No available training agent instance for serviceId=" + serviceId);
        }
        String serviceUri = instances.get(0).getUri().toString();
        if (!StringUtils.hasText(serviceUri)) {
            throw new IllegalStateException("Resolved training agent instance has an empty URI");
        }
        return serviceUri;
    }
}
