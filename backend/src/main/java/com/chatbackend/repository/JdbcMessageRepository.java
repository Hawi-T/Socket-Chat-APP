package com.chatbackend.repository;

import com.chatbackend.entity.Message;
import com.chatbackend.entity.User;
import com.chatbackend.entity.Chat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcMessageRepository {
    
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String dbDriver;

    private Connection getConnection() throws SQLException {
        try {
            // Load the driver here, where dbDriver is accessible
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver not found: " + dbDriver);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public Message save(Message message) {
        String sql = "INSERT INTO messages (id, chat_id, sender_id, content, created_at, status) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection(); // Calls the new getConnection
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            UUID messageId = (message.getId() != null) ? message.getId() : UUID.randomUUID();
            
            pstmt.setString(1, messageId.toString());
            pstmt.setString(2, message.getChat().getId().toString());
            pstmt.setString(3, message.getSender().getId().toString());
            pstmt.setString(4, message.getContent());
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(6, message.getStatus().toString());
            
            pstmt.executeUpdate();
            message.setId(messageId);
            return message;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Message> findByChatId(UUID chatId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u.username as sender_name FROM messages m " +
                    "JOIN users u ON m.sender_id = u.id WHERE m.chat_id = ? ORDER BY m.created_at";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chatId.toString());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Message message = new Message();
                message.setId(UUID.fromString(rs.getString("id")));
                message.setContent(rs.getString("content"));
                message.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                
                User sender = new User();
                sender.setId(UUID.fromString(rs.getString("sender_id")));
                sender.setUsername(rs.getString("sender_name"));
                message.setSender(sender);
                
                messages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
}