# Job Analyzer — Complete Setup & Documentation

Everything needed to run, understand, and troubleshoot the Job Analyzer project.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Ollama Models](#2-ollama-models)
3. [PostgreSQL + PGVector](#3-postgresql--pgvector-docker)
4. [Application Configuration](#4-application-configuration)
5. [Database Schema](#5-database-schema)
6. [Running the Backend](#6-running-the-backend)
7. [Running the Frontend](#7-running-the-frontend)
8. [Verifying Everything Works](#8-verifying-everything-works)
9. [Troubleshooting](#9-troubleshooting)
10. [Environment Variables](#10-environment-variables-reference)
11. [One-Command Reset](#11-one-command-reset-nuclear-option)
12. [Project File Structure](#12-project-file-structure)
13. [Key Dependencies](#13-key-dependencies-pomxml)
14. [Version Compatibility Notes](#14-version-compatibility-notes-important)
15. [GitHub Push Checklist](#15-github-push-checklist)
16. [Docker Compose](#16-docker-compose-optional)
17. [pgAdmin Setup](#17-pgadmin-setup-optional)
18. [.gitignore](#18-gitignore)
19. [README.md](#19-readmemd)

---

## 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java | 21+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker Desktop | Latest | `docker --version` |
| Ollama | Latest | `ollama --version` |
| Node.js (for frontend) | 18+ | `node -v` |

---

## 2. Ollama Models

Two models are required: one for chat (analysis) and one for embeddings (RAG).

```bash
# Chat model — used by JobAnalysisService to analyze resumes
ollama pull llama3

# Embedding model — used by PGVector to vectorize job descriptions
ollama pull nomic-embed-text
```

**Verify both are installed:**
```bash
ollama list
```

Expected output includes:
```
llama3:latest           <id>    4.7 GB
nomic-embed-text:latest <id>    274 MB
```

**Test the embedding endpoint:**
```bash
curl http://localhost:11434/api/embeddings -d "{\"model\": \"nomic-embed-text\", \"prompt\": \"hello\"}"
```
Should return a JSON object with a 768-number `embedding` array.

---

## 3. PostgreSQL + PGVector (Docker)

### Start the container

```bash
docker run -d \
  --name jobanalyzer-postgres \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=jobanalyzer \
  -v jobanalyzer-pgdata:/var/lib/postgresql/data \
  pgvector/pgvector:pg16
```

**What each flag does:**

| Flag | Purpose |
|---|---|
| `--name jobanalyzer-postgres` | Named container — easier to stop/start later |
| `-p 5432:5432` | Maps container port 5432 to localhost:5432 |
| `POSTGRES_PASSWORD=postgres` | Must match `spring.datasource.password` in `application.yaml` |
| `POSTGRES_DB=jobanalyzer` | Must match the database name in the JDBC URL |
| `-v jobanalyzer-pgdata:/var/lib/postgresql/data` | **Persists data** across container restarts — critical |
| `pgvector/pgvector:pg16` | Postgres 16 with PGVector extension pre-installed |

### Verify it's running

```bash
docker ps
```

Look for:
```
CONTAINER ID   IMAGE                     PORTS                    NAMES
<id>           pgvector/pgvector:pg16    0.0.0.0:5432->5432/tcp   jobanalyzer-postgres
```

### Stop / start / remove

```bash
# Stop (data preserved)
docker stop jobanalyzer-postgres

# Start again
docker start jobanalyzer-postgres

# Remove container AND data (fresh start)
docker rm -f jobanalyzer-postgres
docker volume rm jobanalyzer-pgdata
```

### Connect via psql (optional)

```bash
docker exec -it jobanalyzer-postgres psql -U postgres -d jobanalyzer
```

Useful commands:
```sql
\dt                          -- list tables
SELECT * FROM users;         -- registered users
SELECT * FROM analyses;      -- analysis history
SELECT id, LEFT(content, 100) FROM vector_store;  -- stored JDs
\q                           -- quit
```

---

## 4. Application Configuration

### `application.yaml` (committed — no secrets)

```yaml
spring:
  application:
    name: jobanalyzer

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/jobanalyzer}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update

  ai:
    ollama:
      base-url: ${OLLAMA_URL:http://localhost:11434}
      embedding:
        options:
          model: nomic-embed-text
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 768

jwt:
  secret: ${JWT_SECRET:dev-secret-change-me-in-prod-32-chars-min}
  expiration: 3600000
```

### `application-local.yaml` (gitignored — real values)

For local dev with real secrets:

```yaml
jwt:
  secret: MyVeryLongSecretKeyForJwtSigning2026SpringBootProject123
```

Activate it:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Or set env vars in IntelliJ Run Configuration:
```
JWT_SECRET=MyVeryLongSecretKeyForJwtSigning2026SpringBootProject123
```

---

## 5. Database Schema

Created automatically by Hibernate (`ddl-auto: update`) and Spring AI (`initialize-schema: true`).

| Table | Created By | Purpose |
|---|---|---|
| `users` | Hibernate (from `User` entity) | Auth |
| `analyses` | Hibernate (from `Analysis` entity) | Analysis history |
| `job_descriptions` | Hibernate (from `JobDescription` entity) | JD library |
| `vector_store` | Spring AI PGVector | Embeddings for RAG |

**If you need to reset the DB:**
```bash
docker rm -f jobanalyzer-postgres
docker volume rm jobanalyzer-pgdata
# Then re-run the docker run command above
```

---

## 6. Running the Backend

```bash
# From project root
mvn clean compile
mvn spring-boot:run
```

App runs at `http://localhost:8080`.

**Actuator endpoints** (add `management.endpoints.web.exposure.include=*` to yaml if needed):
- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/mappings` — see all endpoints

---

## 7. Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`.

**Important**: The backend CORS config must include `http://localhost:5173`. Check `SecurityConfig.java` → `corsConfigurationSource()`.

---

## 8. Verifying Everything Works

### Smoke test sequence

1. **Register a user:**
   ```bash
   curl -X POST http://localhost:8080/auth/register \
     -H "Content-Type: application/json" \
     -d "{\"username\":\"testuser\",\"password\":\"test1234\"}"
   ```
   Copy the `token` from the response.

2. **Add a JD to the library:**
   ```bash
   curl -X POST http://localhost:8080/jobs/library \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <token>" \
     -d "{\"text\":\"Senior Java Developer with Spring Boot and Kafka experience.\"}"
   ```

3. **Analyze a resume** (use Postman with form-data):
    - `resume` → PDF file
    - `jobDescription` → any JD text
    - Header: `Authorization: Bearer <token>`

   Expected: JSON response with `skills`, `level`, `experienceYears`, `summary`, and `referencedJds` (should include the JD from step 2).

4. **Check history:**
   ```bash
   curl "http://localhost:8080/jobs/analyses?page=0&size=10" \
     -H "Authorization: Bearer <token>"
   ```

---

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `model 'nomic-embed-text' not found` | Embedding model not pulled | `ollama pull nomic-embed-text` |
| `Connection refused: localhost:5432` | Postgres container not running | `docker start jobanalyzer-postgres` |
| `Connection refused: localhost:11434` | Ollama not running | Start Ollama from Start menu |
| `dimensions mismatch` | Wrong `dimensions` in yaml | Set to `768` for `nomic-embed-text` |
| `BeanDefinitionOverrideException` for `vectorStore` | Custom + auto config conflict | Delete `ChromaConfig.java`, remove Chroma deps |
| Slow `/analyze` | Large model + low RAM | Use `llama3.2:3b` or reduce `num_predict` |
| JWT 401 on protected endpoints | Missing or expired token | Re-login, check `Authorization: Bearer <token>` |
| `Method Not Allowed` (405) | Endpoint mapped with wrong HTTP method | Check `@PostMapping` vs `@GetMapping` |
| `Cannot construct instance of OllamaResponse` | Missing no-arg constructor | Add `@NoArgsConstructor` to DTO |

---

## 10. Environment Variables Reference

Set these in IntelliJ Run Config or as system env vars before starting the app:

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | `dev-secret-change-me-in-prod-32-chars-min` | JWT signing key (must be ≥32 chars) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobanalyzer` | Postgres connection |
| `DB_USER` | `postgres` | DB username |
| `DB_PASS` | `postgres` | DB password |
| `OLLAMA_URL` | `http://localhost:11434` | Ollama API base URL |

---

## 11. One-Command Reset (Nuclear Option)

To wipe everything and start fresh:

```bash
# Stop all containers
docker stop jobanalyzer-postgres
docker rm jobanalyzer-postgres
docker volume rm jobanalyzer-pgdata

# Recreate DB
docker run -d --name jobanalyzer-postgres -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=jobanalyzer \
  -v jobanalyzer-pgdata:/var/lib/postgresql/data \
  pgvector/pgvector:pg16

# Restart backend
mvn spring-boot:run
```

All tables will be recreated empty on first startup.

---

## 12. Project File Structure

```
jobanalyzer/
├── src/main/java/com/example/jobanalyzer/
│   ├── JobanalyzerApplication.java
│   ├── controller/         # AuthController, JobAnalyzerController
│   ├── service/            # JwtService, JobAnalysisService, OllamaClient, JdLibraryService, ResumeParserService
│   ├── repository/         # UserRepository, AnalysisRepository, JobDescriptionRepository
│   ├── entity/             # User, Analysis, JobDescription
│   ├── dto/                # LoginRequest, LoginResponse, JobAnalysisResponse, OllamaRequest, OllamaResponse, AnalysisSummary
│   └── security/           # SecurityConfig, JwtAuthenticationFilter
├── src/main/resources/
│   └── application.yaml
├── frontend/               # React (Vite)
│   ├── src/pages/          # Login.jsx, Register.jsx, JobAnalyzer.jsx
│   └── src/App.jsx
├── pom.xml
├── .gitignore
├── SETUP.md                # This file
└── README.md
```

---

## 13. Key Dependencies (pom.xml)

| Dependency | Version | Purpose |
|---|---|---|
| `spring-boot-starter-parent` | 3.3.5 | Spring Boot |
| `spring-ai-starter-model-ollama` | 1.0.6 | Ollama chat + embedding |
| `spring-ai-starter-vector-store-pgvector` | 1.0.6 | PGVector integration |
| `spring-boot-starter-data-jpa` | — | JPA/Hibernate |
| `spring-boot-starter-security` | — | Auth |
| `jjwt-api / impl / jackson` | 0.12.7 | JWT |
| `pdfbox` | 3.0.5 | PDF text extraction |
| `postgresql` (driver) | — | JDBC |
| `lombok` | — | Boilerplate reduction |

---

## 14. Version Compatibility Notes (Important)

This combination was tested and works:

- **Spring Boot 3.3.5** (do NOT upgrade to 3.5.x — Spring AI 1.0.x is not compatible with 3.5.x auto-configuration)
- **Spring AI 1.0.6**
- **PostgreSQL 16** with **PGVector** extension
- **Java 21**
- **ChromaDB removed** — replaced by PGVector due to `Unsupported upgrade request` HTTP client bug

**Do not re-add Chroma.** If you see Chroma references anywhere in code or pom.xml, they are leftovers and should be deleted.

---

## 15. GitHub Push Checklist

Before pushing:

```bash
git status
```

Confirm **NOT** staged:
- [ ] `target/` folder
- [ ] `.idea/` folder
- [ ] `data/` folder (H2 legacy)
- [ ] `application-local.yaml` (real secrets)
- [ ] `*.mv.db` files

Then:

```bash
git add .
git commit -m "Describe your change"
git push
```

---

## 16. Docker Compose (Optional)

For one-command infrastructure, create `docker-compose.yml` in project root:

```yaml
version: "3.9"

services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: jobanalyzer-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: jobanalyzer
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d jobanalyzer"]
      interval: 5s
      timeout: 5s
      retries: 5

  pgadmin:
    image: dpage/pgadmin4
    container_name: jobanalyzer-pgadmin
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@admin.com
      PGADMIN_DEFAULT_PASSWORD: admin
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  pgdata:
```

Usage:
```bash
docker compose up -d     # start everything
docker compose down      # stop (data preserved)
docker compose down -v   # stop + wipe data
```

---

## 17. pgAdmin Setup (Optional)

### Run pgAdmin in Docker

```bash
docker run -d --name pgadmin -p 5050:80 \
  -e 'PGADMIN_DEFAULT_EMAIL=admin@admin.com' \
  -e 'PGADMIN_DEFAULT_PASSWORD=admin' \
  dpage/pgadmin4
```

### Connect to the database

1. Open `http://localhost:5050`
2. Login: `admin@admin.com` / `admin`
3. Right-click **Servers** → **Register** → **Server**
4. In the **Connection** tab, enter:
    - **Host**: `host.docker.internal` (needed because pgAdmin is in a container — `localhost` won't work)
    - **Port**: `5432`
    - **Database**: `jobanalyzer`
    - **Username**: `postgres`
    - **Password**: `postgres`
5. Save

### Navigate to tables

Expand the tree in this order:
- **Servers** → your server name
- **Databases** → `jobanalyzer`
- **Schemas** → `public`
- **Tables**

You should see `users`, `analyses`, `job_descriptions`, and `vector_store`.

---

## 18. `.gitignore`

Create this file in project root to prevent secrets and build artifacts from being committed:

```gitignore
# Build output
target/
*.class

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Local data & secrets
data/
*.mv.db
*.trace.db

# Environment & config with secrets
application-local.yaml
application-prod.yaml
application-secret.yaml
.env
.env.local

# Logs
*.log
logs/

# Database dumps
*.dump
*.sql.gz
```

---

## 19. `README.md`

Create this file in project root — it's what recruiters read first.

```markdown
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

```
┌─────────────┐
│  React UI   │
└──────┬──────┘
│ JWT
┌──────▼──────────────────────────────────┐
│         Spring Boot Backend             │
│  Controllers → Services → Repositories  │
│                    │                    │
│           ┌────────┴─────────┐          │
│           │                  │          │
│   OllamaClient          VectorStore     │
│           │                  │          │
└───────────┼──────────────────┼──────────┘
│                  │
┌───────▼──────┐  ┌────────▼────────┐
│   Ollama     │  │   PGVector      │
│  (LLM +      │  │  (JD embeddings)│
│  embeddings) │  └─────────────────┘
└──────────────┘
```

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/register` | Create account, returns JWT |
| POST | `/auth/login` | Authenticate, returns JWT |
| POST | `/jobs/analyze` | RAG-based resume analysis |
| POST | `/jobs/library` | Add JD to retrieval corpus |
| GET | `/jobs/analyses` | Paginated analysis history |

## Running Locally

See **[SETUP.md](./SETUP.md)** for the complete setup guide — every Docker command, Ollama model, config file, and troubleshooting step.

Quick start (if you already have the prerequisites installed):

```bash
# 1. Pull Ollama models
ollama pull llama3 && ollama pull nomic-embed-text

# 2. Start Postgres + PGVector
docker run -d --name jobanalyzer-postgres -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=jobanalyzer \
  -v jobanalyzer-pgdata:/var/lib/postgresql/data \
  pgvector/pgvector:pg16

# 3. Set JWT secret (IntelliJ Run Config or terminal)
export JWT_SECRET="your-32-char-secret-here"

# 4. Run backend
mvn spring-boot:run

# 5. Run frontend (separate terminal)
cd frontend && npm install && npm run dev
```
