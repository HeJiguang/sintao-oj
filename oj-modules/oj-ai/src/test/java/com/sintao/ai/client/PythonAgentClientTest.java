package com.sintao.ai.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.sintao.ai.config.AgentFacadeProperties;
import com.sintao.ai.domain.dto.AiChatRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonAgentClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldPostChatRequestToConfiguredChatEndpoint() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<Headers> requestHeaders = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat-endpoint", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(readBody(exchange));
            requestHeaders.set(exchange.getRequestHeaders());
            respondJson(exchange, """
                    {
                      "trace_id": "t-1",
                      "intent": "analyze_failure",
                      "answer": "python-answer"
                    }
                    """);
        });
        server.start();

        AgentFacadeProperties properties = new AgentFacadeProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setChatPath("/chat-endpoint");

        PythonAgentClient client = new PythonAgentClient(properties);
        AiChatRequest request = buildRequest();

        String answer = client.chat(request);

        assertEquals("python-answer", answer);
        assertEquals("/chat-endpoint", requestPath.get());
        assertTrue(requestBody.get().contains("\"question_title\":\"Two Sum\""));
        assertTrue(requestBody.get().contains("\"user_message\":\"Why is this WA?\""));
        assertNull(requestHeaders.get().getFirst("Upgrade"));
        assertNull(requestHeaders.get().getFirst("HTTP2-Settings"));
        assertFalse(String.valueOf(requestHeaders.get().getFirst("Connection")).contains("Upgrade"));
    }

    @Test
    void shouldCallConfiguredStreamEndpoint() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/stream-endpoint", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            byte[] body = (
                    "event: meta\n" +
                    "data: {\"trace_id\":\"t-1\"}\n\n" +
                    "event: final\n" +
                    "data: {\"answer\":\"stream-answer\"}\n\n"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        AgentFacadeProperties properties = new AgentFacadeProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setStreamPath("/stream-endpoint");

        PythonAgentClient client = new PythonAgentClient(properties);
        List<String> chunks = client.streamChat(buildRequest()).collectList().block();
        String combined = String.join("", chunks);

        assertEquals("/stream-endpoint", requestPath.get());
        assertTrue(combined.contains("event: final"), combined);
        assertTrue(combined.contains("stream-answer"), combined);
    }

    private AiChatRequest buildRequest() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestionTitle("Two Sum");
        request.setQuestionContent("Find two numbers that add up to target.");
        request.setUserCode("public class Solution {}");
        request.setJudgeResult("WA on sample #2");
        request.setUserMessage("Why is this WA?");
        return request;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respondJson(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
