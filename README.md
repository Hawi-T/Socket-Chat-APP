# Negarit

## Negarit is Full-Stack Real-Time Chat System with WebSocket Persistence

A robust, containerized chat application featuring real-time bi-directional communication, JWT-based authentication, and persistent storage in MySQL.

## 🚀 Quick Start (Docker One-Click)

The entire environment is orchestrated via Docker. Ensure you have **Docker Desktop** installed and running.

1. **Clone/Unzip** the project.
2. **Open a terminal** in the project root.
3. **Build and start** the services:
   ```bash
   docker-compose up --build
             or
   docker compose up --build

## Access the Application:

- Frontend: http://localhost:3000
- Backend_API: http://localhost:8080

## 🏗 Architecture & Tech Stack

- Backend (Java / Spring Boot)
- Spring WebSocket: Handles full-duplex communication.
- DBC Persistence: Custom JdbcMessageRepository ensures every WebSocket message is stored in MySQL before broadcast.
- JWT (jjwt): Stateless security for both REST endpoints and WebSocket handshakes.
- Jackson: High-performance JSON processing.

- **Frontend (React / Vite)**
- Vite: Modern build tool configured with Terser to automatically strip console.log and debugger statements in production builds.
- WebSocket API: Native browser implementation for low-latency message delivery.
- Tailwind CSS: For a responsive, modern UI.

- **Database (MySQL 8.0)**
- Uses UUIDs (Binary 16) for primary keys to ensure global uniqueness and scalability.
- Automated schema initialization via docker-entrypoint-initdb.d.

## 🛠 Features
1. WebSocket Persistence Bridge
Unlike basic chat apps that only broadcast messages in memory, this system implements a "Save-then-Broadcast" pattern in ChatWebSocketHandler.java. This ensures that when a user refreshes their page, the chat history remains intact.

2. Thread-Safe Session Management
Implemented synchronized blocks within the WebSocket handler to prevent IllegalStateException during high-concurrency message broadcasts.

3. Production-Ready Dockerization
Multi-stage Builds: The backend uses a Maven build stage and a lightweight JRE run stage to keep image sizes small.

Environment Isolation: Database credentials and JWT secrets are managed via environment variables in the docker-compose.yml.

📂 Project Structure
Plaintext
```
/
├── frontend/      # React source code & Vite configuration
├── backend/            # Spring Boot source code (Java)
└── docker-compose.yml  # System orchestrator
```
