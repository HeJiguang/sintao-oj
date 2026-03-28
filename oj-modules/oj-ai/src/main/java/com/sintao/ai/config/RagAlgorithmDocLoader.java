package com.sintao.ai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads markdown algorithm notes from the classpath and ingests them into the
 * vector store used by the RAG advisor.
 */
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
                log.info("RAG: no algorithm documents found, skipping vector ingestion.");
                return;
            }

            List<Document> documents = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                String content = readUtf8(resource);
                String filename = resource.getFilename();
                if (content == null || content.isBlank()) {
                    continue;
                }
                documents.add(new Document(content, Map.of("source", filename != null ? filename : "unknown")));
            }

            if (documents.isEmpty()) {
                log.info("RAG: algorithm documents were found but no readable content was ingested.");
                return;
            }

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(300)
                    .withMinChunkSizeChars(50)
                    .build();

            List<Document> chunks = splitter.apply(documents);
            vectorStore.add(chunks);
            log.info("RAG: loaded {} source documents and ingested {} chunks.", documents.size(), chunks.size());
        } catch (IOException e) {
            log.warn("RAG: failed to read algorithm documents.", e);
        } catch (Exception e) {
            log.warn("RAG: failed to ingest algorithm documents into the vector store.", e);
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
