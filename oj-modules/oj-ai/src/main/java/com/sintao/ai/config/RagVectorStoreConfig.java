package com.sintao.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.vectorstore.SearchRequest;

/**
 * RAG 向量库配置�?
 * 使用 SimpleVectorStore（内存，重启后清空），适合开�?演示�?
 * 生产可改�?Redis Stack（spring-ai-starter-vector-store-redis）或 PGVector（spring-ai-starter-vector-store-pgvector），
 * 由各�?starter 自动配置 VectorStore，本类可去掉或仅提供 Advisor�?
 */
@Configuration
public class RagVectorStoreConfig {

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /** RAG 顾问：根据用户问题从向量库检索相关文档并拼入上下文。可通过 spring.ai.rag.enabled=false 关闭�?*/
    @Bean
    @ConditionalOnProperty(name = "spring.ai.rag.enabled", havingValue = "true", matchIfMissing = true)
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(6)
                        .similarityThreshold(0.6)
                        .build())
                .build();
    }
}

