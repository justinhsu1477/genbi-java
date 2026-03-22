package com.lndata.genbi.service.mock;

import com.lndata.genbi.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Embedding Mock 實作 — 回傳隨機向量 (dev 環境)
 */
@Slf4j
@Service
@Profile("dev")
public class MockEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 1536;
    private final Random random = new Random(42);

    @Override
    public List<Float> createEmbedding(String text) {
        log.info("[Mock Embedding] createEmbedding: text={}", text.length() > 50 ? text.substring(0, 50) + "..." : text);
        List<Float> embedding = new ArrayList<>(DIMENSION);
        for (int i = 0; i < DIMENSION; i++) {
            embedding.add(random.nextFloat() * 2 - 1);
        }
        return embedding;
    }
}
