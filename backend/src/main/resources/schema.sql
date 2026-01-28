CREATE DATABASE IF NOT EXISTS chatdb;
USE chatdb;

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS chat_participants;
DROP TABLE IF EXISTS chats;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP NULL
);

-- Create chats table
CREATE TABLE chats (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create chat_participants table (junction table for many-to-many)
CREATE TABLE chat_participants (
    chat_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_id, user_id),
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create messages table
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'SENT',
    -- Index to optimize querying messages by chat and ordering by time
    INDEX idx_chat_created (chat_id, created_at), 
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);
