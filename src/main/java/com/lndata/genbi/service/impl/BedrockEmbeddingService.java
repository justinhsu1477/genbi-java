package com.lndata.genbi.service.impl;

import com.lndata.genbi.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AWS Bedrock Embedding 服務 — 透過 Spring AI EmbeddingModel 將文字轉為向量
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class BedrockEmbeddingService implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Override
    public List<Float> createEmbedding(String text) {
        log.info("[Embedding] createEmbedding: textLength={}", text.length());
        try {
            float[] embedding = embeddingModel.embed(text);

            // float[] → List<Float>
            List<Float> result = new java.util.ArrayList<>(embedding.length);
            for (float v : embedding) {
                result.add(v);
            }

            log.debug("[Embedding] dimension={}", result.size());
            return result;
        } catch (Exception e) {
            log.error("[Embedding] createEmbedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding invocation failed: " + e.getMessage(), e);
        }
    }
}
