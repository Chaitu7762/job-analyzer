# Job Analyzer — AI Resume Analysis with RAG

An AI-powered job application assistant that analyzes a resume against a job description using **Retrieval-Augmented Generation (RAG)**.

## What It Does

1. User uploads a resume (PDF) and pastes a job description
2. Resume text is embedded and used to retrieve the **3 most similar job descriptions** from a corpus stored in a vector database
3. Retrieved JDs are injected into the prompt for **grounded, market-aware analysis**
4. LLM returns structured JSON: extracted skills, seniority level, experience years, summary
5. Every analysis is persisted for history, caching, and evaluation

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3.5, Java 21 |
| Security | Spring Security + JWT |
| AI | Spring AI 1.0.6, Ollama (`llama3` + `nomic-embed-text`) |
| Vector DB | PostgreSQL 16 + PGVector |
| Relational DB | PostgreSQL 16 |
| PDF Parsing | Apache PDFBox |
| Frontend | React (Vite) |

## Architecture
┌─────────────┐
│ React UI │
└──────┬──────┘
│ JWT
┌──────▼──────────────────────────────────┐
│ Spring Boot Backend │
│ Controllers → Services → Repositories │
│ │ │
│ ┌────────┴─────────┐ │
│ │ │ │
│ OllamaClient VectorStore │
│ │ │ │
└───────────┼──────────────────┼──────────┘
│ │
┌───────▼──────┐ ┌────────▼────────┐
│ Ollama │ │ PGVector │
│ (LLM + │ │ (JD embeddings)│
│ embeddings) │ └─────────────────┘
└──────────────┘


## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/register` | Create account, returns JWT |
| POST | `/auth/login` | Authenticate, returns JWT |
| POST | `/jobs/analyze` | RAG-based resume analysis |
| POST | `/jobs/library` | Add JD to retrieval corpus |
| GET | `/jobs/analyses` | Paginated analysis history |

## Running Locally

### Prerequisites
- Java 21
- Maven 3.9+
- Docker Desktop
- Ollama installed and running

### Setup

1. **Pull models**:
   ```bash
   ollama pull llama3
   ollama pull nomic-embed-text
