package com.nlq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenSearch 設定 — 向量搜索 / RAG
 */
@ConfigurationProperties(prefix = "nlq.opensearch")
public record OpenSearchProperties(
        String host,
        int port,
        String username,
        String password,
        String scheme,
        String sqlIndex,
        String nerIndex,
        String agentIndex,
        String logIndex
) {
    public OpenSearchProperties {
        if (host == null) host = "localhost";
        if (port <= 0) port = 9200;
        if (scheme == null) scheme = "https";
        if (sqlIndex == null) sqlIndex = "uba";
        if (nerIndex == null) nerIndex = "uba_ner";
        if (agentIndex == null) agentIndex = "uba_agent";
        if (logIndex == null) logIndex = "genbi_query_logging";
    }
}
