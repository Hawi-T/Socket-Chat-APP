package com.chatbackend.controller;

import com.chatbackend.dto.LoginRequest;
import com.chatbackend.dto.RegisterRequest;
import com.chatbackend.dto.ApiResponse;
import com.chatbackend.entity.User;
import com.chatbackend.repository.UserRepository;
import com.chatbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    @Autowired
    public AuthController(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtUtil jwtUtil,
                         AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        // Basic input validation
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Username is required"));
        }
        
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Password must be at least 6 characters"));
        }
        
        // Check if username exists in the database
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Username already exists"));
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName() != null ? request.getName() : request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl(request.getAvatarUrl());
        
        // Save to MySQL database
        user = userRepository.save(user);
        
        // Generate REAL JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId().toString());
        
        Map<String, Object> data = new HashMap<>();
        data.put("user", mapUserToResponse(user));
        data.put("token", token);
        
        return ResponseEntity.ok(new ApiResponse(true, "Registration successful", data));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Find user details
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "User not found"));
            }
            
            User user = userOpt.get();
            
            // Update last seen
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            
            // Generate REAL JWT token
            String token = jwtUtil.generateToken(user.getUsername(), user.getId().toString());
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", mapUserToResponse(user));
            data.put("token", token);
            
            return ResponseEntity.ok(new ApiResponse(true, "Login successful", data));
            
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .body(new ApiResponse(false, "Invalid credentials: " + e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        // Extract token
        String token = authHeader.replace("Bearer ", "").trim();
        
        try {
            // Validate token
            String username = jwtUtil.extractUsername(token);
            String userId = jwtUtil.extractUserId(token);
            
            if (username == null || userId == null) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "Invalid token"));
            }
            
            // Validate token
            if (!jwtUtil.validateToken(token, username)) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "Token expired or invalid"));
            }
            
            Optional<User> userOpt = userRepository.findById(UUID.fromString(userId));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "User not found"));
            }
            
            User user = userOpt.get();
            
            // Update last seen
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", mapUserToResponse(user));
            
            return ResponseEntity.ok(new ApiResponse(true, "User retrieved", data));
            
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .body(new ApiResponse(false, "Authentication failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        
        try {
            String username = jwtUtil.extractUsername(token);
            String userId = jwtUtil.extractUserId(token);
            
            if (!jwtUtil.validateToken(token, username)) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "Token expired or invalid"));
            }
            
            // Generate new token
            String newToken = jwtUtil.generateToken(username, userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            
            return ResponseEntity.ok(new ApiResponse(true, "Token refreshed", data));
            
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .body(new ApiResponse(false, "Token refresh failed"));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        
        try {
            String username = jwtUtil.extractUsername(token);
            
            if (jwtUtil.validateToken(token, username)) {
                return ResponseEntity.ok(new ApiResponse(true, "Token is valid"));
            } else {
                return ResponseEntity.status(401)
                    .body(new ApiResponse(false, "Token is invalid"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .body(new ApiResponse(false, "Token validation failed"));
        }
    }
    
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse> searchUsers(@RequestParam String query) {
        // Search users in MySQL database
        var users = userRepository.searchUsers(query);
        
        Map<String, Object> data = new HashMap<>();
        data.put("users", users.stream().map(this::mapUserToResponse).toList());
        
        return ResponseEntity.ok(new ApiResponse(true, "Users found", data));
    }
    
    /**
     * Helper method to map User entity to a response DTO (excluding password hash).
     */
    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("username", user.getUsername());
        userMap.put("name", user.getName());
        userMap.put("avatarUrl", user.getAvatarUrl());
        // Determine online status: seen within the last 5 minutes
        userMap.put("isOnline", user.getLastSeen() != null && 
            user.getLastSeen().isAfter(LocalDateTime.now().minusMinutes(5)));
        userMap.put("lastSeen", user.getLastSeen());
        return userMap;
    }
}