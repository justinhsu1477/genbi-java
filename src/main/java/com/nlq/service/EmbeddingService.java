package com.nlq.service;

import java.util.List;

/**
 * 向量嵌入服務介面 — 將文字轉為向量 (Embedding)
 */
public interface EmbeddingService {

    /**
     * 將文字轉為向量
     *
     * @param text 要向量化的文字
     * @return 向量 (1536 維 float 陣列)
     */
    List<Float> createEmbedding(String text);
}
