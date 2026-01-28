package com.chatbackend.config;

import com.chatbackend.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket handler
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*"); // Allows all origins for development
        
        // // Optional: Add SockJS fallback for browsers without WebSocket support
        // registry.addHandler(chatWebSocketHandler, "/ws/chat")
        //         .setAllowedOriginPatterns("*")
        //         .withSockJS();
            
    }
}