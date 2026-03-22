package com.nlq.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenSearch Client 設定 — 建立與 OpenSearch 叢集的連線
 *
 * 本地 Docker: scheme=http (security disabled)
 * AWS OpenSearch Service: scheme=https (IAM/Basic Auth)
 */
@Slf4j
@Configuration
@Profile("!dev")
public class OpenSearchConfig {

    @Bean(destroyMethod = "close")
    public RestHighLevelClient openSearchClient(OpenSearchProperties props) {
        log.info("[OpenSearch] Initializing client: host={}, port={}, scheme={}",
                props.host(), props.port(), props.scheme());

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(props.username(), props.password())
        );

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(props.host(), props.port(), props.scheme()))
                        .setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
        );

        log.info("[OpenSearch] Client initialized successfully");
        return client;
    }
}
