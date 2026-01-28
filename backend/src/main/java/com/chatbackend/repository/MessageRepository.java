package com.chatbackend.repository;

import com.chatbackend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt ASC")
    List<Message> findByChatIdOrderByCreatedAtAsc(@Param("chatId") UUID chatId);
    
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLastMessageByChatId(@Param("chatId") UUID chatId);
    
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND m.createdAt > :since ORDER BY m.createdAt ASC")
    List<Message> findNewMessages(@Param("chatId") UUID chatId, @Param("since") LocalDateTime since);
    
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND m.sender.id = :senderId AND m.status = 'SENT'")
    List<Message> findUnreadMessages(@Param("chatId") UUID chatId, @Param("senderId") UUID senderId);
}