package com.nlq.config;

import com.nlq.controller.QaWebSocketHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 - 註冊 /qa/ws 端點
 * WebSocket config — register /qa/ws endpoint
 */
@Configuration
@EnableWebSocket
@AllArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final QaWebSocketHandler qaWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(qaWebSocketHandler, "/qa/ws")
                .setAllowedOrigins("*"); // 開發階段允許所有來源 allow all origins for dev
    }
}
