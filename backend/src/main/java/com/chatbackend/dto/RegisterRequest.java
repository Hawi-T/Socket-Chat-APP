package com.chatbackend.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String avatarUrl;
    private String name; // Add this field
    
    // Constructors
    public RegisterRequest() {}
    
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}