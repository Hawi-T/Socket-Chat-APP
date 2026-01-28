// entity/User.java
package com.chatbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    /**
     * Set creation and initial last seen time before the entity is persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // lastSeen is set to current time on creation as the user is "seen" upon creation
        lastSeen = LocalDateTime.now(); 
    }
    
    /**
     * Update last seen time before the entity is updated (e.g., on login or activity).
     */
    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }
    
    // Constructors
    public User() {}
    
    public User(String username, String name, String passwordHash) {
        this.username = username;
        this.name = name;
        this.passwordHash = passwordHash;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}