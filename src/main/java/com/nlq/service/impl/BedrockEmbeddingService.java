package com.nlq.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlq.config.BedrockProperties;
import com.nlq.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

/**
 * AWS Bedrock Titan Embedding 服務 — 將文字轉為 1536 維向量
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class BedrockEmbeddingService implements EmbeddingService {

    private static final String TITAN_EMBEDDING_MODEL = "amazon.titan-embed-text-v1";

    private final BedrockProperties bedrockProperties;
    private final ObjectMapper objectMapper;

    private volatile BedrockRuntimeClient client;

    @Override
    public List<Float> createEmbedding(String text) {
        log.info("[Embedding] createEmbedding: textLength={}", text.length());
        try {
            Map<String, Object> body = Map.of("inputText", text);
            String bodyJson = objectMapper.writeValueAsString(body);

            InvokeModelResponse response = getClient().invokeModel(InvokeModelRequest.builder()
                    .modelId(TITAN_EMBEDDING_MODEL)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(bodyJson))
                    .build());

            String responseJson = response.body().asUtf8String();
            Map<String, Object> responseMap = objectMapper.readValue(responseJson, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<Number> rawEmbedding = (List<Number>) responseMap.get("embedding");

            List<Float> embedding = rawEmbedding.stream()
                    .map(Number::floatValue)
                    .toList();

            log.debug("[Embedding] dimension={}", embedding.size());
            return embedding;

        } catch (Exception e) {
            log.error("[Embedding] createEmbedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding invocation failed: " + e.getMessage(), e);
        }
    }

    /** 懶初始化 Bedrock client */
    private BedrockRuntimeClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = BedrockRuntimeClient.builder()
                            .region(Region.of(bedrockProperties.region()))
                            .build();
                    log.info("[Embedding] Bedrock client initialized: region={}", bedrockProperties.region());
                }
            }
        }
        return client;
    }
}
