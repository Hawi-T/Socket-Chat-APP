package com.chatbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "chats")
public class Chat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    private UUID id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private ChatType type;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "chat_participants",
        joinColumns = @JoinColumn(name = "chat_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();
    
    /**
     * Set creation time before the entity is persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum ChatType {
        PRIVATE, GROUP
    }
    
    // Constructors, Getters, Setters
    public Chat() {}
    
    public Chat(String name, ChatType type) {
        this.name = name;
        this.type = type;
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public ChatType getType() { return type; }
    public void setType(ChatType type) { this.type = type; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Set<User> getParticipants() { return participants; }
    public void setParticipants(Set<User> participants) { this.participants = participants; }
}