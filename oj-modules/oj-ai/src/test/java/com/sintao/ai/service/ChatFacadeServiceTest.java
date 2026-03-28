package com.sintao.ai.service;

import com.sintao.ai.client.PythonAgentClient;
import com.sintao.ai.config.AgentFacadeProperties;
import com.sintao.ai.domain.agent.AgentChatResponse;
import com.sintao.ai.domain.dto.AiChatRequest;
import com.sintao.ai.domain.dto.AiChatDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFacadeServiceTest {

    @Mock
    private ObjectProvider<AiChatService> legacyAiChatServiceProvider;

    @Mock
    private PythonAgentClient pythonAgentClient;

    @Mock
    private AgentFacadeProperties properties;

    @Mock
    private AiChatService legacyAiChatService;

    private ChatFacadeService chatFacadeService;

    @BeforeEach
    void setUp() {
        chatFacadeService = new ChatFacadeService(legacyAiChatServiceProvider, pythonAgentClient, properties);
    }

    @Test
    void shouldDelegateToPythonClientWhenLanggraphModeEnabled() {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("Why is this WA?");

        when(properties.getMode()).thenReturn("langgraph");
        when(pythonAgentClient.chat(request)).thenReturn("python-answer");

        String answer = chatFacadeService.chat(request);

        assertEquals("python-answer", answer);
        verify(pythonAgentClient).chat(request);
        verifyNoInteractions(legacyAiChatServiceProvider, legacyAiChatService);
    }

    @Test
    void shouldReturnPythonChatDetailWhenLanggraphModeEnabled() {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("Why is this WA?");

        AgentChatResponse response = new AgentChatResponse();
        response.setTraceId("trace-001");
        response.setIntent("analyze_failure");
        response.setAnswer("python-answer");
        response.setConfidence(0.86);
        response.setNextAction("Trace the smallest failing example.");

        when(properties.getMode()).thenReturn("langgraph");
        when(pythonAgentClient.chatDetail(request)).thenReturn(response);

        AiChatDetailResponse detail = chatFacadeService.chatDetail(request);

        assertEquals("python-answer", detail.getAnswer());
        assertEquals("analyze_failure", detail.getIntent());
        assertEquals(0.86, detail.getConfidence());
        assertEquals("Trace the smallest failing example.", detail.getNextAction());
        verify(pythonAgentClient).chatDetail(request);
        verifyNoInteractions(legacyAiChatServiceProvider, legacyAiChatService);
    }

    @Test
    void shouldDelegateToLegacyServiceWhenLegacyModeEnabled() {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("Explain this problem.");

        when(properties.getMode()).thenReturn("legacy");
        when(legacyAiChatServiceProvider.getIfAvailable()).thenReturn(legacyAiChatService);
        when(legacyAiChatService.chat(request)).thenReturn("legacy-answer");

        String answer = chatFacadeService.chat(request);

        assertEquals("legacy-answer", answer);
        verify(legacyAiChatServiceProvider).getIfAvailable();
        verify(legacyAiChatService).chat(request);
    }

    @Test
    void shouldBuildLegacyChatDetailWhenLegacyModeEnabled() {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("Explain this problem.");

        when(properties.getMode()).thenReturn("legacy");
        when(legacyAiChatServiceProvider.getIfAvailable()).thenReturn(legacyAiChatService);
        when(legacyAiChatService.chat(request)).thenReturn("legacy-answer");

        AiChatDetailResponse detail = chatFacadeService.chatDetail(request);

        assertEquals("legacy-answer", detail.getAnswer());
        assertEquals("legacy", detail.getMode());
        assertEquals("legacy_response", detail.getIntent());
        assertEquals(1.0, detail.getConfidence());
        verify(legacyAiChatServiceProvider).getIfAvailable();
        verify(legacyAiChatService).chat(request);
    }

    @Test
    void shouldDelegateStreamToPythonClientWhenLanggraphModeEnabled() {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("Why is this WA?");

        Flux<String> upstream = Flux.just(
                "event: status\ndata: {\"node\":\"router\"}\n\n",
                "event: final\ndata: {\"answer\":\"python-answer\"}\n\n"
        );

        when(properties.getMode()).thenReturn("langgraph");
        when(pythonAgentClient.streamChat(request)).thenReturn(upstream);

        List<String> chunks = chatFacadeService.streamChat(request).collectList().block();

        assertEquals(upstream.collectList().block(), chunks);
        verify(pythonAgentClient).streamChat(request);
        verifyNoInteractions(legacyAiChatServiceProvider, legacyAiChatService);
    }

    @Test
    void shouldWrapLegacyAnswerAsSseWhenLegacyModeEnabled() {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("Explain this problem.");

        when(properties.getMode()).thenReturn("legacy");
        when(legacyAiChatServiceProvider.getIfAvailable()).thenReturn(legacyAiChatService);
        when(legacyAiChatService.chat(request)).thenReturn("legacy-answer");

        List<String> chunks = chatFacadeService.streamChat(request).collectList().block();

        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("event: meta")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("event: status")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("event: final")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("\"answer\":\"legacy-answer\"")));
        verify(legacyAiChatServiceProvider).getIfAvailable();
        verify(legacyAiChatService).chat(request);
    }
}
