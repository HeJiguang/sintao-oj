package com.sintao.ai.controller;

import com.sintao.ai.service.ChatFacadeService;
import com.sintao.ai.domain.dto.AiChatDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AiChatController.class,
        properties = {
                "spring.cloud.bootstrap.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false"
        }
)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatFacadeService chatFacadeService;

    @Test
    void shouldReturnJsonChatResponse() throws Exception {
        when(chatFacadeService.chat(any())).thenReturn("legacy-answer");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userMessage": "Explain this problem."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("legacy-answer"));
    }

    @Test
    void shouldReturnJsonChatDetailResponse() throws Exception {
        AiChatDetailResponse detail = new AiChatDetailResponse();
        detail.setTraceId("trace-001");
        detail.setMode("langgraph");
        detail.setIntent("analyze_failure");
        detail.setAnswer("python-answer");
        detail.setConfidence(0.88);
        detail.setNextAction("Trace the smallest failing example.");

        when(chatFacadeService.chatDetail(any())).thenReturn(detail);

        mockMvc.perform(post("/chat/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userMessage": "Explain this problem."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("python-answer"))
                .andExpect(jsonPath("$.data.intent").value("analyze_failure"))
                .andExpect(jsonPath("$.data.nextAction").value("Trace the smallest failing example."));
    }

    @Test
    void shouldReturnSseStreamResponse() throws Exception {
        when(chatFacadeService.streamChat(any())).thenReturn(Flux.just(
                "event: meta\ndata: {\"trace_id\":\"t-1\"}\n\n",
                "event: final\ndata: {\"answer\":\"stream-answer\"}\n\n"
        ));

        MvcResult mvcResult = mockMvc.perform(post("/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "userMessage": "Explain this problem."
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event: final")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("stream-answer")));
    }
}
