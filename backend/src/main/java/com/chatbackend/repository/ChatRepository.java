package com.chatbackend.repository;

import com.chatbackend.entity.Chat;
import com.chatbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {
    
    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.id = :userId ORDER BY (SELECT MAX(m.createdAt) FROM Message m WHERE m.chat = c) DESC NULLS LAST")
    List<Chat> findChatsByUserId(@Param("userId") UUID userId);
    
    List<Chat> findByType(Chat.ChatType type);
    
    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants AND SIZE(c.participants) = 2")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Chat c WHERE c.id = :chatId AND :user MEMBER OF c.participants")
    boolean isUserInChat(@Param("chatId") UUID chatId, @Param("user") User user);
}