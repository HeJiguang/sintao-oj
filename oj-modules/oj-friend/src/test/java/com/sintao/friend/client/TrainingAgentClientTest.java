package com.sintao.friend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sintao.friend.client.dto.AgentTrainingPlanResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TrainingAgentClientTest {

    @Test
    void objectMapperShouldDeserializeDueTimeAndIgnoreFutureFields() throws Exception {
        String payload = """
                {
                  "current_level": "starter",
                  "target_direction": "algorithm_foundation",
                  "weak_points": "binary search timing",
                  "strong_points": "array basics",
                  "plan_title": "Starter Recovery Cycle",
                  "plan_goal": "Rebuild stability.",
                  "ai_summary": "Keep the cycle tight.",
                  "future_summary_hint": "ignore me",
                  "tasks": [
                    {
                      "task_type": "test",
                      "exam_id": 9001,
                      "title_snapshot": "Binary Search Checkpoint",
                      "task_order": 3,
                      "recommended_reason": "Check the current rhythm.",
                      "knowledge_tags_snapshot": "binary search",
                      "due_time": "2026-03-26T10:30:00",
                      "priority_score": 0.93
                    }
                  ]
                }
                """;

        ObjectMapper objectMapper = (ObjectMapper) ReflectionTestUtils.getField(TrainingAgentClient.class, "OBJECT_MAPPER");

        AgentTrainingPlanResponse response = objectMapper.readValue(payload, AgentTrainingPlanResponse.class);

        assertEquals("Starter Recovery Cycle", response.getPlanTitle());
        assertEquals(1, response.getTasks().size());
        assertNotNull(response.getTasks().get(0).getDueTime());
        assertEquals(LocalDateTime.of(2026, 3, 26, 10, 30), response.getTasks().get(0).getDueTime());
    }

    @Test
    void resolvePlanUrlShouldUseDiscoveryClientWhenExplicitBaseUrlIsBlank() {
        TrainingAgentClient client = new TrainingAgentClient();
        ReflectionTestUtils.setField(client, "baseUrl", "");
        ReflectionTestUtils.setField(client, "planPath", "/api/training/plan");
        ReflectionTestUtils.setField(client, "serviceId", "oj-agent");
        ReflectionTestUtils.setField(client, "discoveryClient", new DiscoveryClient() {
            @Override
            public String description() {
                return "test-discovery";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return List.of(new ServiceInstance() {
                    @Override
                    public String getServiceId() {
                        return "oj-agent";
                    }

                    @Override
                    public String getHost() {
                        return "127.0.0.1";
                    }

                    @Override
                    public int getPort() {
                        return 8000;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public URI getUri() {
                        return URI.create("http://127.0.0.1:8000");
                    }

                    @Override
                    public java.util.Map<String, String> getMetadata() {
                        return java.util.Collections.emptyMap();
                    }
                });
            }

            @Override
            public List<String> getServices() {
                return List.of("oj-agent");
            }
        });

        String planUrl = (String) ReflectionTestUtils.invokeMethod(client, "resolvePlanUrl");

        assertEquals("http://127.0.0.1:8000/api/training/plan", planUrl);
    }
}
