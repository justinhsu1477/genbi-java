package com.lndata.genbi.service.mock;

import com.lndata.genbi.dto.*;
import com.lndata.genbi.service.SampleManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 範例管理 Mock 實作 — 用 in-memory Map 模擬 (dev 環境)
 */
@Slf4j
@Service
@Profile("dev")
public class MockSampleManagementService implements SampleManagementService {

    private final Map<String, Map<String, Object>> sqlSamples = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> entitySamples = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> agentSamples = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<SampleResponse> getAllSqlSamples(String profileName) {
        log.info("[Mock Sample] getAllSqlSamples: profileName={}", profileName);
        return filterByProfile(sqlSamples, profileName);
    }

    @Override
    public void addSqlSample(SqlSampleRequest request) {
        log.info("[Mock Sample] addSqlSample: profile={}, question={}", request.profileName(), request.question());
        String id = "mock-sql-" + idGen.getAndIncrement();
        sqlSamples.put(id, Map.of(
                "text", request.question(),
                "sql", request.sql(),
                "profile", request.profileName()
        ));
    }

    @Override
    public void deleteSqlSample(String profileName, String docId) {
        log.info("[Mock Sample] deleteSqlSample: docId={}", docId);
        sqlSamples.remove(docId);
    }

    @Override
    public List<SampleResponse> getAllEntitySamples(String profileName) {
        log.info("[Mock Sample] getAllEntitySamples: profileName={}", profileName);
        return filterByProfile(entitySamples, profileName);
    }

    @Override
    public void addEntitySample(EntitySampleRequest request) {
        log.info("[Mock Sample] addEntitySample: profile={}, entity={}", request.profileName(), request.entity());
        String id = "mock-ner-" + idGen.getAndIncrement();
        entitySamples.put(id, Map.of(
                "entity", request.entity(),
                "comment", request.comment(),
                "entity_type", request.entityType(),
                "profile", request.profileName()
        ));
    }

    @Override
    public void deleteEntitySample(String profileName, String docId) {
        log.info("[Mock Sample] deleteEntitySample: docId={}", docId);
        entitySamples.remove(docId);
    }

    @Override
    public List<SampleResponse> getAllAgentCotSamples(String profileName) {
        log.info("[Mock Sample] getAllAgentCotSamples: profileName={}", profileName);
        return filterByProfile(agentSamples, profileName);
    }

    @Override
    public void addAgentCotSample(AgentCotSampleRequest request) {
        log.info("[Mock Sample] addAgentCotSample: profile={}, query={}", request.profileName(), request.query());
        String id = "mock-agent-" + idGen.getAndIncrement();
        agentSamples.put(id, Map.of(
                "query", request.query(),
                "comment", request.comment(),
                "profile", request.profileName()
        ));
    }

    @Override
    public void deleteAgentCotSample(String profileName, String docId) {
        log.info("[Mock Sample] deleteAgentCotSample: docId={}", docId);
        agentSamples.remove(docId);
    }

    private List<SampleResponse> filterByProfile(Map<String, Map<String, Object>> store, String profileName) {
        return store.entrySet().stream()
                .filter(e -> profileName.equals(e.getValue().get("profile")))
                .map(e -> new SampleResponse(e.getKey(), e.getValue()))
                .toList();
    }
}
