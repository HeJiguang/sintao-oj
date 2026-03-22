package com.sintao.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 算法资料加载器：�?classpath:algorithm-docs/** 读取 Markdown 等文本，
 * 切分后写�?VectorStore，用于基于算法资料的问答�? * 仅当 spring.ai.rag.enabled=true 时生效；�?Embedding 不可用（如仅配置�?DeepSeek Chat），需关闭 RAG 避免 404�? */
@Component
@ConditionalOnProperty(name = "spring.ai.rag.enabled", havingValue = "true")
public class RagAlgorithmDocLoader {

    private static final Logger log = LoggerFactory.getLogger(RagAlgorithmDocLoader.class);
    private static final String ALGORITHM_DOCS_LOCATION = "classpath:algorithm-docs/**/*.md";

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resourcePatternResolver;

    public RagAlgorithmDocLoader(VectorStore vectorStore,
                                 ResourcePatternResolver resourcePatternResolver) {
        this.vectorStore = vectorStore;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @PostConstruct
    public void loadAndIngest() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(ALGORITHM_DOCS_LOCATION);
            if (resources.length == 0) {
                log.info("RAG: 未找到算法资料文件，跳过入库。可�?resources/algorithm-docs/ 下添�?.md 文件�?);
                return;
            }
            List<Document> documents = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable()) continue;
                String content = readUtf8(resource);
                String filename = resource.getFilename();
                if (content == null || content.isBlank()) continue;
                documents.add(new Document(content, Map.of("source", filename != null ? filename : "unknown")));
            }
            if (documents.isEmpty()) {
                log.info("RAG: 无有效算法资料内容，跳过入库�?);
                return;
            }
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(300)
                    .withMinChunkSizeChars(50)
                    .build();
            List<Document> chunks = splitter.apply(documents);
            vectorStore.add(chunks);
            log.info("RAG: 已加�?{} 个算法资料文件，切分�?{} 个片段并写入向量库�?, documents.size(), chunks.size());
        } catch (IOException e) {
            log.warn("RAG: 加载算法资料失败，将不提供基于资料的检索�?, e);
        } catch (Exception e) {
            log.warn("RAG: 算法资料入库失败（若未配�?Embedding 接口可忽略）�?, e);
        }
    }

    private static String readUtf8(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}

