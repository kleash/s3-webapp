package com.example.s3webapp.config;

import com.example.s3webapp.foldersize.FolderSizeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FolderSizeWebSocketHandler folderSizeWebSocketHandler;
    private final AppProperties appProperties;

    public WebSocketConfig(FolderSizeWebSocketHandler folderSizeWebSocketHandler, AppProperties appProperties) {
        this.folderSizeWebSocketHandler = folderSizeWebSocketHandler;
        this.appProperties = appProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(folderSizeWebSocketHandler, "/api/ws/folder-size/{jobId}")
                .setAllowedOrigins(appProperties.cors().allowedOrigins().toArray(String[]::new));
    }
}
