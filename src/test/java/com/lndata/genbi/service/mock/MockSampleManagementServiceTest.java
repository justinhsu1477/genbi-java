package com.lndata.genbi.service.mock;

import com.lndata.genbi.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockSampleManagementService 測試 — 驗證 in-memory CRUD 邏輯
 */
class MockSampleManagementServiceTest {

    private MockSampleManagementService service;

    @BeforeEach
    void setUp() {
        service = new MockSampleManagementService();
    }

    @Nested
    @DisplayName("SQL 範例 CRUD")
    class SqlSampleCrudTest {

        @Test
        @DisplayName("新增後可查詢到")
        void addAndGet() {
            service.addSqlSample(new SqlSampleRequest("demo", "total sales", "SELECT SUM(amount) FROM orders"));
            service.addSqlSample(new SqlSampleRequest("demo", "top products", "SELECT product_id FROM orders"));
            service.addSqlSample(new SqlSampleRequest("other", "count users", "SELECT COUNT(*) FROM users"));

            List<SampleResponse> demoSamples = service.getAllSqlSamples("demo");
            assertEquals(2, demoSamples.size());

            List<SampleResponse> otherSamples = service.getAllSqlSamples("other");
            assertEquals(1, otherSamples.size());
        }

        @Test
        @DisplayName("刪除後查詢不到")
        void addAndDelete() {
            service.addSqlSample(new SqlSampleRequest("demo", "test", "SELECT 1"));
            List<SampleResponse> before = service.getAllSqlSamples("demo");
            assertEquals(1, before.size());

            service.deleteSqlSample("demo", before.get(0).id());
            List<SampleResponse> after = service.getAllSqlSamples("demo");
            assertTrue(after.isEmpty());
        }

        @Test
        @DisplayName("空 profile → 回傳空列表")
        void emptyProfile() {
            List<SampleResponse> result = service.getAllSqlSamples("nonexistent");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Entity 範例 CRUD")
    class EntitySampleCrudTest {

        @Test
        @DisplayName("新增 Entity 後可查詢")
        void addAndGet() {
            service.addEntitySample(new EntitySampleRequest("demo", "台積電", "客戶", "metrics", List.of()));
            List<SampleResponse> result = service.getAllEntitySamples("demo");
            assertEquals(1, result.size());
            assertEquals("台積電", result.get(0).source().get("entity"));
        }
    }

    @Nested
    @DisplayName("Agent COT 範例 CRUD")
    class AgentCotCrudTest {

        @Test
        @DisplayName("新增 Agent COT 後可查詢")
        void addAndGet() {
            service.addAgentCotSample(new AgentCotSampleRequest("demo", "compare Q1 Q2", "1. Q1, 2. Q2"));
            List<SampleResponse> result = service.getAllAgentCotSamples("demo");
            assertEquals(1, result.size());
            assertEquals("compare Q1 Q2", result.get(0).source().get("query"));
        }

        @Test
        @DisplayName("Profile 隔離 — 不同 profile 互不影響")
        void profileIsolation() {
            service.addAgentCotSample(new AgentCotSampleRequest("p1", "q1", "c1"));
            service.addAgentCotSample(new AgentCotSampleRequest("p2", "q2", "c2"));

            assertEquals(1, service.getAllAgentCotSamples("p1").size());
            assertEquals(1, service.getAllAgentCotSamples("p2").size());
        }
    }
}
