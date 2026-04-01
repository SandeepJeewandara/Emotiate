# Emotiate - Hotel Price Negotiation Backend

## Introduction

Emotiate is a Spring Boot backend system that powers an AI-driven hotel price negotiation service. The system detects customer emotions from conversation input and uses those signals to adjust pricing strategies in real time. Each negotiation runs through a multi-agent pipeline built on JADE (Java Agent Development Framework), where agents communicate using FIPA ACL messages and interact with a large language model via the Groq API.

The backend exposes REST endpoints for managing users, rooms, packages, bookings, and negotiation sessions, alongside a WebSocket interface for real-time chat.

---

## Features

- Emotion detection from guest messages using a Groq-hosted LLM
- Dynamic pricing strategy selection based on emotional state, negotiation round, and budget constraints
- Multi-agent negotiation system using JADE with FSM-based seller agent behavior
- Real-time bidirectional communication over WebSocket with STOMP
- JWT-based authentication with role-based access control (Admin, Staff, Guest)
- Persistent storage of negotiation sessions, chat history, bookings, and inventory

---

## Technology Stack

| Category | Technology |
|---|---|
| Framework | Spring Boot 4.0.3 |
| Language | Java 21 |
| Multi-Agent System | JADE 4.6.0 |
| Database | PostgreSQL |
| ORM | Spring Data JPA (Hibernate) |
| Security | Spring Security + JJWT 0.12.5 |
| LLM Provider | Groq API (llama-3.3-70b-versatile) |
| Real-time | WebSocket + STOMP + SockJS |
| HTTP Client | Spring WebFlux (WebClient) |
| Build Tool | Maven |
| Utilities | Lombok, Jackson |

---

## Architecture Overview

The system follows a layered architecture with a multi-agent core.

```
Frontend (WebSocket / REST)
        |
Spring Controllers (REST API)
        |
Spring Services (Business Logic)
        |
JADE Agent Layer (UserAgent <-> SellerAgent <-> ResourceMgtAgent)
        |
AI Modules (Emotion Detection, Strategy Optimization, Seller Reply)
        |
Groq LLM API + PostgreSQL
```

**Agent Roles:**

- `UserAgent` - Created per session. Receives guest messages from Spring via Object-to-Agent (O2A) communication and forwards them to the SellerAgent using CFP messages.
- `SellerAgent` - FSM-based agent that moves through five phases: `GET_INFO`, `RECOMMEND_PACKAGE`, `NEGOTIATING`, `BOOKING`, and `CONFIRMATION`. Runs all three AI modules per round.
- `ResourceMgtAgent` - Singleton agent that manages room and package inventory. Registered with the JADE Directory Facilitator.

**AI Modules:**

- `EmotionDetectionModule` - Sends the guest message to Groq and parses the returned emotion label and confidence score.
- `StrategyOptimizationModule` - Pure Java calculation using VAD (Valence-Arousal-Dominance) scores, round decay, and budget factors to select a negotiation strategy.
- `SellerReplyModule` - Generates the agent's conversational reply and price offer using a phase-specific LLM prompt.

---

## Project Structure

```
src/main/java/com/project/Emotiate/
├── agent/          # JADE agent classes
├── config/         # Spring configuration (Security, WebSocket, JADE, CORS)
├── controller/     # REST controllers
├── dto/            # Request and response DTOs, organized by domain
├── entity/         # JPA entities
├── enums/          # Domain enumerations
├── exception/      # Exception handling
├── module/         # AI modules (emotion, strategy, reply)
├── repository/     # Spring Data JPA repositories
├── security/       # JWT filter and utilities
├── service/        # Business logic services
└── util/           # Shared utilities

src/main/resources/
├── application.properties
└── prompts/        # LLM prompt templates loaded at runtime
```

---

## Setup and Installation

**Requirements:**

- Java 21
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL 14+
- A Groq API key

**Steps:**

1. Clone the repository.

2. Create a PostgreSQL database:
   ```sql
   CREATE DATABASE "Emotiate_DB";
   ```

3. Create a `.env` file in the project root:
   ```env
   JWT_SECRET=your_base64_encoded_secret
   GROQ_API_KEY=your_groq_api_key
   ```

4. Confirm that `lib/jade.jar` is present. This file is required as a local Maven dependency and must not be deleted.

5. Install dependencies:
   ```bash
   ./mvnw clean install
   ```

---
