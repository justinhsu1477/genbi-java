package com.lndata.genbi.service.impl;

import com.lndata.genbi.service.EmbeddingService;
import com.lndata.genbi.service.mock.MockEmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingService 單元測試 — 用 MockEmbeddingService 驗證行為
 */
class BedrockEmbeddingServiceTest {

    private final EmbeddingService embeddingService = new MockEmbeddingService();

    @Test
    @DisplayName("createEmbedding: 回傳 1536 維向量")
    void createEmbedding_shouldReturn1536Dimensions() {
        List<Float> result = embeddingService.createEmbedding("test query about monthly revenue");

        assertNotNull(result);
        assertEquals(1536, result.size());
    }

    @Test
    @DisplayName("createEmbedding: 空字串仍回傳向量")
    void createEmbedding_emptyText_stillReturnsVector() {
        List<Float> result = embeddingService.createEmbedding("");

        assertNotNull(result);
        assertEquals(1536, result.size());
    }

    @Test
    @DisplayName("createEmbedding: 向量值在 -1 到 1 之間")
    void createEmbedding_valuesInRange() {
        List<Float> result = embeddingService.createEmbedding("test");

        for (Float val : result) {
            assertTrue(val >= -1.0f && val <= 1.0f,
                    "Embedding value should be between -1 and 1, got: " + val);
        }
    }
}
