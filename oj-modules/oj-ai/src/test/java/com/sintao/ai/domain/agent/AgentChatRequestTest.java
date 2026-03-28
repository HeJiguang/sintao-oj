package com.sintao.ai.domain.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sintao.ai.domain.dto.AiChatRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentChatRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldIncludeOjContextFieldsWhenBuildingAgentRequest() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestionTitle("Two Sum");
        request.setQuestionContent("Find two numbers that add up to target.");
        request.setUserCode("public class Solution {}");
        request.setJudgeResult("WA on sample #2");
        request.setUserMessage("Why is this WA?");

        AgentChatRequest agentRequest = AgentChatRequest.from(request);
        Map<String, Object> payload = objectMapper.convertValue(
                agentRequest,
                new TypeReference<>() {
                }
        );

        assertEquals("Two Sum", payload.get("question_title"));
        assertEquals("Find two numbers that add up to target.", payload.get("question_content"));
        assertEquals("public class Solution {}", payload.get("user_code"));
        assertEquals("WA on sample #2", payload.get("judge_result"));
        assertEquals("Why is this WA?", payload.get("user_message"));
    }
}
