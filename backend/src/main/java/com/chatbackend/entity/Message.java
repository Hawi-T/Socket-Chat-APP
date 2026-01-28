// entity/Message.java
package com.chatbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
    
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    private MessageStatus status;
    
    // JDBC: Timestamp and Enum handling
    // Network: Message as data packet
    
    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = MessageStatus.SENT;
        }
    }
    
    // Constructors, Getters, Setters
    public Message() {}
    
    public Message(Chat chat, User sender, String content) {
        this.chat = chat;
        this.sender = sender;
        this.content = content;
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
}