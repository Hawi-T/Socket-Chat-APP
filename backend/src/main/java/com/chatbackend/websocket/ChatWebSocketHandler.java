package com.chatbackend.websocket;

import com.chatbackend.entity.Message;
import com.chatbackend.entity.Chat;
import com.chatbackend.entity.User;
import com.chatbackend.repository.JdbcMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private JdbcMessageRepository messageRepository;
    
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private static final Map<String, String> userIdToUsername = new ConcurrentHashMap<>();
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private Key jwtSigningKey;
    
    public ChatWebSocketHandler() {}

    @PostConstruct
    public void init() {
        try {
            if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
                throw new IllegalStateException("JWT Secret is not configured");
            }
            byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            this.jwtSigningKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            System.err.println("Error initializing JWT key: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            String token = extractTokenFromQuery(query);
            
            if (token != null) {
                String userId = extractUserIdFromJwtToken(token);
                String username = extractUsernameFromJwtToken(token);
                
                if (userId != null && username != null) {
                    userSessions.put(userId, session);
                    sessionToUser.put(session.getId(), userId);
                    userIdToUsername.put(userId, username);
                    
                    sendWelcomeMessage(session, username);
                    notifyUserStatus(userId, username, true);
                    sendConnectionConfirmation(session, userId, username);
                } else {
                    closeSessionSafely(session, new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Invalid token"));
                }
            } else {
                sendAuthRequired(session);
            }
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            JsonNode node = mapper.readTree(payload);
            String type = node.get("type").asText();
            
            switch (type) {
                case "MESSAGE":
                    handleChatMessage(session, node.get("payload"));
                    break;
                case "TYPING":
                    handleTypingIndicator(session, node.get("payload"));
                    break;
                case "READ_RECEIPT":
                    handleReadReceipt(session, node.get("payload"));
                    break;
                case "AUTH":
                    handleAuth(session, node.get("payload"));
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    safeSendError(session, "Unknown type: " + type);
            }
        } catch (Exception e) {
            safeSendError(session, "Internal error");
        }
    }
    
    private void handleAuth(WebSocketSession session, JsonNode payload) throws IOException {
        try {
            String token = payload.get("token").asText();
            String userId = extractUserIdFromJwtToken(token);
            String username = extractUsernameFromJwtToken(token);
            
            if (userId != null && validateJwtToken(token)) {
                userSessions.put(userId, session);
                sessionToUser.put(session.getId(), userId);
                userIdToUsername.put(userId, username);
                
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "AUTH_SUCCESS");
                response.put("userId", userId);
                response.put("username", username);
                sendMessageSafely(session, response.toString());
                notifyUserStatus(userId, username, true);
            }
        } catch (Exception e) {
            safeSendError(session, "Auth failed");
        }
    }

    private void handleChatMessage(WebSocketSession session, JsonNode payload) throws IOException {
        String senderId = sessionToUser.get(session.getId());
        if (senderId == null) {
            safeSendError(session, "Not authenticated");
            return;
        }

        String chatId = payload.get("chatId").asText();
        String text = payload.get("text").asText();
        String username = userIdToUsername.get(senderId);

        // --- FIX: Using the correct Enum reference ---
        Message messageEntity = new Message();
        
        Chat chat = new Chat();
        chat.setId(UUID.fromString(chatId));
        messageEntity.setChat(chat);

        User sender = new User();
        sender.setId(UUID.fromString(senderId));
        messageEntity.setSender(sender);

        messageEntity.setContent(text);
        
        // This is the specific fix for your compilation error:
        messageEntity.setStatus(Message.MessageStatus.SENT); 

        Message savedMessage = messageRepository.save(messageEntity);
        
        if (savedMessage == null) {
            safeSendError(session, "Database save failed");
            return;
        }

        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("id", savedMessage.getId().toString());
        messageNode.put("chatId", chatId);
        messageNode.put("senderId", senderId);
        messageNode.put("senderName", username != null ? username : "Unknown");
        messageNode.put("text", text);
        messageNode.put("timestamp", System.currentTimeMillis());
        messageNode.put("status", "SENT");
        messageNode.put("type", "text");
        
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "NEW_MESSAGE");
        response.set("payload", messageNode);
        
        broadcastToAll(response.toString());
        sendDeliveryConfirmation(session, messageNode.get("id").asText(), chatId);
    }

    private void broadcastToAll(String message) {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) sendMessageSafely(session, message);
        });
    }

    private void broadcastToOthers(String excludeSessionId, String message) {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen() && !session.getId().equals(excludeSessionId)) {
                sendMessageSafely(session, message);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionToUser.get(session.getId());
        if (userId != null) {
            String username = userIdToUsername.get(userId);
            userSessions.remove(userId);
            sessionToUser.remove(session.getId());
            notifyUserStatus(userId, username, false);
        }
    }

    private synchronized void sendMessageSafely(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }

    private void safeSendError(WebSocketSession session, String errorMessage) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "ERROR");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("message", errorMessage);
        response.set("payload", payload);
        sendMessageSafely(session, response.toString());
    }

    private void closeSessionSafely(WebSocketSession session, CloseStatus closeStatus) {
        try { if (session.isOpen()) session.close(closeStatus); } catch (IOException e) {}
    }

    private String extractUserIdFromJwtToken(String token) {
        try {
            if (token.startsWith("mock-jwt-token-")) return token.substring(15);
            Claims claims = Jwts.parserBuilder().setSigningKey(jwtSigningKey).build().parseClaimsJws(token).getBody();
            return claims.get("userId", String.class);
        } catch (Exception e) { return null; }
    }

    private String extractUsernameFromJwtToken(String token) {
        try {
            if (token.startsWith("mock-jwt-token-")) return "User_" + token.substring(15, 19);
            Claims claims = Jwts.parserBuilder().setSigningKey(jwtSigningKey).build().parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception e) { return null; }
    }

    private boolean validateJwtToken(String token) {
        try {
            if (token.startsWith("mock-jwt-token-")) return true;
            Jwts.parserBuilder().setSigningKey(jwtSigningKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) { return false; }
    }

    private void handlePing(WebSocketSession session) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "PONG");
        sendMessageSafely(session, response.toString());
    }

    private void sendWelcomeMessage(WebSocketSession session, String username) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "SYSTEM");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("content", "Welcome, " + username);
        response.set("payload", payload);
        sendMessageSafely(session, response.toString());
    }

    private void sendConnectionConfirmation(WebSocketSession session, String userId, String username) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "CONNECTION_SUCCESS");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("userId", userId);
        payload.put("username", username);
        response.set("payload", payload);
        sendMessageSafely(session, response.toString());
    }

    private void sendAuthRequired(WebSocketSession session) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "AUTH_REQUIRED");
        sendMessageSafely(session, response.toString());
    }

    private void sendDeliveryConfirmation(WebSocketSession session, String messageId, String chatId) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "MESSAGE_DELIVERED");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("messageId", messageId);
        payload.put("chatId", chatId);
        response.set("payload", payload);
        sendMessageSafely(session, response.toString());
    }

    private void notifyUserStatus(String userId, String username, boolean isOnline) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "USER_STATUS");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("isOnline", isOnline);
        response.set("payload", payload);
        broadcastToAll(response.toString());
    }

    private String extractTokenFromQuery(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) return java.net.URLDecoder.decode(param.substring(6), java.nio.charset.StandardCharsets.UTF_8);
        }
        return null;
    }

    private void handleTypingIndicator(WebSocketSession session, JsonNode payload) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "TYPING");
        response.set("payload", payload);
        broadcastToOthers(session.getId(), response.toString());
    }

    private void handleReadReceipt(WebSocketSession session, JsonNode payload) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "READ_RECEIPT");
        response.set("payload", payload);
        broadcastToAll(response.toString());
    }
}