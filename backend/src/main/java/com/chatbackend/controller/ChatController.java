package com.chatbackend.controller;

import com.chatbackend.dto.ApiResponse;
import com.chatbackend.entity.Chat;
import com.chatbackend.entity.Message;
import com.chatbackend.entity.User;
import com.chatbackend.dto.ApiResponse;
import com.chatbackend.repository.ChatRepository;
import com.chatbackend.repository.MessageRepository;
import com.chatbackend.repository.UserRepository;
import com.chatbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {
    
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    
    @Autowired
    public ChatController(ChatRepository chatRepository, 
                          MessageRepository messageRepository,
                          UserRepository userRepository,
                          JwtUtil jwtUtil) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse> getChats(@RequestHeader("Authorization") String authHeader) {
        try {
            UUID userId = extractUserIdFromToken(authHeader);
            
            // Get user's chats from MySQL database
            List<Chat> chats = chatRepository.findChatsByUserId(userId);
            
            // Enrich chats with last message
            List<Map<String, Object>> chatResponses = chats.stream().map(chat -> {
                Map<String, Object> chatMap = new HashMap<>();
                chatMap.put("id", chat.getId().toString());
                chatMap.put("name", chat.getName());
                chatMap.put("type", chat.getType().toString());
                chatMap.put("avatarUrl", chat.getAvatarUrl());
                
                // Get last message
                Optional<Message> lastMessageOpt = messageRepository.findLastMessageByChatId(chat.getId());
                if (lastMessageOpt.isPresent()) {
                    Message msg = lastMessageOpt.get();
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("id", msg.getId().toString());
                    messageMap.put("chatId", msg.getChat().getId().toString());
                    messageMap.put("senderId", msg.getSender().getId().toString());
                    messageMap.put("text", msg.getContent());
                    // Convert LocalDateTime to Epoch Milliseconds
                    messageMap.put("timestamp", msg.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
                    messageMap.put("status", msg.getStatus().toString());
                    messageMap.put("type", "text");
                    chatMap.put("lastMessage", messageMap);
                }
                
                // Get participants
                List<Map<String, Object>> participants = chat.getParticipants().stream()
                    .map(this::mapUserToResponse)
                    .collect(Collectors.toList());
                chatMap.put("participants", participants);
                
                // Mock unread count (in real app, calculate from message status)
                chatMap.put("unreadCount", 0);
                // chatMap.put("lastMessage", latestMessage);
                
                return chatMap;
            }).collect(Collectors.toList());
            
            Map<String, Object> data = new HashMap<>();
            data.put("chats", chatResponses);
            
            return ResponseEntity.ok(new ApiResponse(true, "Chats retrieved", data));
            
        } catch (Exception e) {
            // Error handling for authentication failure
            return ResponseEntity.status(401)
                .body(new ApiResponse(false, "Authentication failed"));
        }
    }
    
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse> getMessages(
            @PathVariable String chatId,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            UUID userId = extractUserIdFromToken(authHeader);
            UUID chatUUID = UUID.fromString(chatId);
            
            // Verify user has access to chat
            Optional<Chat> chatOpt = chatRepository.findById(chatUUID);
            if (chatOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "Chat not found"));
            }
            
            // Check if user is participant 
            boolean isParticipant = chatOpt.get().getParticipants().stream()
                .anyMatch(user -> user.getId().equals(userId));
            
            if (!isParticipant) {
                return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "Access denied"));
            }
            
            // Get messages from MySQL database, sorted by creation time
            List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatUUID);
            
            // Convert to frontend format
            List<Map<String, Object>> messageResponses = messages.stream().map(msg -> {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("id", msg.getId().toString());
                messageMap.put("chatId", msg.getChat().getId().toString());
                messageMap.put("senderId", msg.getSender().getId().toString());
                messageMap.put("text", msg.getContent());
                messageMap.put("timestamp", msg.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
                messageMap.put("status", msg.getStatus().toString());
                messageMap.put("type", "text");
                return messageMap;
            }).collect(Collectors.toList());
            
            Map<String, Object> data = new HashMap<>();
            data.put("messages", messageResponses);
            
            return ResponseEntity.ok(new ApiResponse(true, "Messages retrieved", data));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Invalid chat ID"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Server error: " + e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse> createChat(
            @RequestBody CreateChatRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            UUID userId = extractUserIdFromToken(authHeader);
            Optional<User> currentUserOpt = userRepository.findById(userId);
            
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "User not found"));
            }
            
            User currentUser = currentUserOpt.get();
            
            if ("PRIVATE".equals(request.getType())) {
                // Create private chat
                if (request.getParticipantIds().size() != 1) {
                    return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Private chat requires exactly one other participant"));
                }
                
                UUID otherUserId = UUID.fromString(request.getParticipantIds().get(0));
                Optional<User> otherUserOpt = userRepository.findById(otherUserId);
                
                if (otherUserOpt.isEmpty()) {
                    return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Participant not found"));
                }
                
                // Check if private chat already exists (requires a custom query in ChatRepository)
                Optional<Chat> existingChat = chatRepository.findPrivateChatBetweenUsers(
                    currentUser, otherUserOpt.get());
                
                if (existingChat.isPresent()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("chatId", existingChat.get().getId().toString());
                    return ResponseEntity.ok(new ApiResponse(true, "Chat already exists", data));
                }
                
                // Create new private chat
                Chat chat = new Chat();
                chat.setName(request.getName() != null ? request.getName() : 
                    currentUser.getName() + " & " + otherUserOpt.get().getName());
                chat.setType(Chat.ChatType.PRIVATE);
                chat.setParticipants(new HashSet<>(Arrays.asList(currentUser, otherUserOpt.get())));
                
                chat = chatRepository.save(chat);
                
                Map<String, Object> data = new HashMap<>();
                data.put("chatId", chat.getId().toString());
                
                return ResponseEntity.ok(new ApiResponse(true, "Private chat created", data));
                
            } else if ("GROUP".equals(request.getType())) {
                // Create group chat
                Chat chat = new Chat();
                chat.setName(request.getName());
                chat.setType(Chat.ChatType.GROUP);
                
                // Add current user
                Set<User> participants = new HashSet<>();
                participants.add(currentUser);
                
                // Add other participants
                for (String participantId : request.getParticipantIds()) {
                    Optional<User> userOpt = userRepository.findById(UUID.fromString(participantId));
                    userOpt.ifPresent(participants::add);
                }
                
                chat.setParticipants(participants);
                chat = chatRepository.save(chat);
                
                Map<String, Object> data = new HashMap<>();
                data.put("chatId", chat.getId().toString());
                
                return ResponseEntity.ok(new ApiResponse(true, "Group chat created", data));
            } else {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid chat type"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Error creating chat: " + e.getMessage()));
        }
    }

    private UUID extractUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        // Use your existing JwtUtil to get the userId claim
        String userIdStr = jwtUtil.extractUserId(token); 
        return UUID.fromString(userIdStr);
    }
    
    /**
     * Helper method to map User entity to a response DTO (excluding sensitive data).
     */
    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("name", user.getName());
        userMap.put("username", user.getUsername());
        userMap.put("avatarUrl", user.getAvatarUrl());
        // Determine online status: seen within the last 5 minutes
        userMap.put("isOnline", user.getLastSeen() != null && 
            user.getLastSeen().isAfter(LocalDateTime.now().minusMinutes(5)));
        return userMap;
    }
    
    // Request DTOs
    public static class CreateChatRequest {
        private String name;
        private String type; // "PRIVATE" or "GROUP"
        private List<String> participantIds;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public List<String> getParticipantIds() { return participantIds; }
        public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }
    }
    
    public static class MessageRequest {
        private String content;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}