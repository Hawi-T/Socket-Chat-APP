package com.chatbackend.websocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtValidator {
    
    @Value("${jwt.secret}")
    private String secret;
    
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String validateAndExtractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.get("userId", String.class);
            
        } catch (Exception e) {
            System.err.println("JWT validation error: " + e.getMessage());
            return null;
        }
    }
}