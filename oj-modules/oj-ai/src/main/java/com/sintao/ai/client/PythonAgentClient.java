package com.sintao.ai.client;

import com.sintao.ai.config.AgentFacadeProperties;
import com.sintao.ai.domain.agent.AgentChatRequest;
import com.sintao.ai.domain.agent.AgentChatResponse;
import com.sintao.ai.domain.dto.AiChatRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

@Component
public class PythonAgentClient {

    private final AgentFacadeProperties properties;

    private final WebClient webClient;

    public PythonAgentClient(AgentFacadeProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .build()
                ))
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public String chat(AiChatRequest request) {
        AgentChatResponse response = chatDetail(request);

        if (response == null || !StringUtils.hasText(response.getAnswer())) {
            throw new IllegalStateException("Python agent returned an empty answer");
        }
        return response.getAnswer();
    }

    public AgentChatResponse chatDetail(AiChatRequest request) {
        return webClient.post()
                .uri(properties.getResolvedChatPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(AgentChatRequest.from(request))
                .retrieve()
                .bodyToMono(AgentChatResponse.class)
                .block();
    }

    public Flux<String> streamChat(AiChatRequest request) {
        return webClient.post()
                .uri(properties.getResolvedStreamPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(AgentChatRequest.from(request))
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(this::readChunk);
    }

    private String readChunk(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
