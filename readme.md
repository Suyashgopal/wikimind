# WikiMind

WikiMind is a Retrieval-Augmented Generation (RAG) service that answers questions
using only the content of documents you provide, instead of a language model's
general training data. Text is ingested, split into chunks, embedded as vectors,
and stored in PostgreSQL using the PGVector extension. When a question is asked,
the most relevant chunks are retrieved by similarity search and used as grounding
context for the answer.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Authentication and Authorization](#authentication-and-authorization)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Deployment](#deployment)
- [Roadmap](#roadmap)
- [License](#license)

## Overview

Standard language models only know what they were trained on. WikiMind closes
that gap using the RAG pattern:

| Step | Description |
|---|---|
| Retrieve | Find the most relevant stored document chunks for a given question |
| Augment | Insert those chunks into the prompt as context |
| Generate | Ask the language model to answer using only that context |

This keeps answers grounded in the caller's own data and reduces hallucination,
without needing to fine-tune a model.

## Architecture

```
                 ingest                                  ask
                   |                                       |
                   v                                       v
          +-----------------+                    +-----------------+
          |  RagController  |                    |  RagController  |
          +-----------------+                    +-----------------+
                   |                                       |
                   v                                       v
          +-----------------------------------------------------+
          |                     RagService                       |
          |  chunk -> embed -> store        embed -> retrieve    |
          |                                  -> prompt -> answer |
          +-----------------------------------------------------+
                   |                     |                 |
                   v                     v                 v
        +-------------------+  +----------------+  +----------------+
        |   EmbeddingModel  |  | WikiDocument   |  |   ChatModel    |
        |   (Gemini)        |  | Repository     |  |   (Gemini)     |
        +-------------------+  +----------------+  +----------------+
                                        |
                                        v
                          +---------------------------+
                          | PostgreSQL + PGVector      |
                          | wiki_documents(id, text,   |
                          | source_title, embedding)   |
                          +---------------------------+
```

Requests are authenticated with a JWT before reaching `RagController`; the token
carries the user's ID, organization, and role, which are used to scope which
documents a query is allowed to retrieve.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 25 |
| Application Framework | Spring Boot 3.5 |
| AI Integration | Spring AI 1.1, Google Gemini (`gemini-2.5-flash`, `text-embedding-004`) |
| Persistence | Spring Data JPA, Hibernate, `hibernate-vector` |
| Database | PostgreSQL 16 with the PGVector extension |
| Authentication | Spring Security, JSON Web Tokens |
| API Documentation | springdoc-openapi (Swagger UI) |
| Containerization | Docker, Docker Compose |
| Testing | JUnit 5, Mockito |
| Build Tool | Maven (via Maven Wrapper) |

## Project Structure

```
wikimind/
  src/main/java/com/wikimind/
    controller/     REST endpoints
    service/        RAG pipeline and authentication logic
    repository/     Spring Data JPA repositories
    model/          JPA entities
    dto/            Request and response payloads
    security/       JWT filter, security configuration
    exception/      Custom exceptions and global handler
  src/main/resources/
    application.properties
  src/test/java/com/wikimind/
    ...             Unit and integration tests
  docker-compose.yml
  .env.example
  pom.xml
```

## Getting Started

### Prerequisites

| Requirement | Purpose |
|---|---|
| JDK 25 | Compiling and running the application |
| Docker Desktop | Running PostgreSQL with PGVector locally |
| Gemini API key | Chat and embedding model access ([Google AI Studio](https://aistudio.google.com/apikey)) |

### Steps

1. Clone the repository:

   ```bash
   git clone https://github.com/Suyashgopal/wikimind.git
   cd wikimind
   ```

2. Start the database:

   ```bash
   docker compose up -d
   ```

3. Copy the environment template and fill in your values:

   ```bash
   cp .env.example .env
   ```

4. Export the required variables (or set them in your IDE's run configuration):

   ```bash
   setx GEMINI_API_KEY "your-key-here"
   ```

5. Run the application:

   ```bash
   ./mvnw spring-boot:run
   ```

6. The API is available at `http://localhost:8080`, and interactive documentation
   at `http://localhost:8080/swagger-ui.html`.

## Configuration

All configuration is supplied through environment variables, with local defaults
defined in `application.properties`.

| Variable | Description | Default |
|---|---|---|
| `GEMINI_API_KEY` | API key for Google Gemini chat and embedding models | none, required |
| `DB_URL` | JDBC connection string for PostgreSQL | `jdbc:postgresql://localhost:5432/wikimind` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | Signing key for issued JWTs | none, required |
| `JWT_EXPIRATION_MS` | Access token lifetime, in milliseconds | `3600000` |

## API Reference

All endpoints other than authentication require a valid JWT in the
`Authorization: Bearer <token>` header.

### Authentication

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create a new user account |
| POST | `/api/auth/login` | Authenticate and receive a JWT |

### Documents

| Method | Path | Description | Required Role |
|---|---|---|---|
| POST | `/api/rag/add` | Ingest text: chunk, embed, and store it | `MEMBER` |
| POST | `/api/rag/ask` | Ask a question, answered using retrieved context | `MEMBER` |
| GET | `/api/rag/documents` | List documents owned by the caller's organization | `MEMBER` |
| DELETE | `/api/rag/documents/{id}` | Remove a stored document | `ADMIN` |

#### Example: ingest text

```http
POST /api/rag/add
Content-Type: application/json
Authorization: Bearer <token>

{
  "content": "The company's remote work policy allows up to three days per week from home."
}
```

#### Example: ask a question

```http
POST /api/rag/ask
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": "How many remote days are employees allowed?"
}
```

Response:

```json
{
  "answer": "Employees are allowed up to three remote work days per week."
}
```

## Authentication and Authorization

Authentication is stateless, using JSON Web Tokens issued at login and validated
on every subsequent request by a servlet filter registered in the Spring
Security filter chain.

| Role | Permissions |
|---|---|
| `ADMIN` | Full access, including deleting documents and managing users within an organization |
| `MEMBER` | Ingest documents and ask questions scoped to their own organization |

Each `WikiDocument` is tagged with the organization ID of the user who created
it. Retrieval queries are always filtered by the caller's organization,
preventing one organization's data from being returned in another's answers.

## Error Handling

A global exception handler returns a consistent error structure for all
failures:

```json
{
  "timestamp": "2026-09-18T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "content must not be blank",
  "path": "/api/rag/add"
}
```

| Status | Condition |
|---|---|
| 400 | Validation failure on the request body |
| 401 | Missing or invalid JWT |
| 403 | Valid JWT, insufficient role |
| 404 | Requested document does not exist |
| 500 | Unexpected failure, including upstream Gemini API errors |

## Testing

```bash
./mvnw test
```

Unit tests cover chunking, prompt construction, and repository query logic
using JUnit 5 and Mockito. Integration tests validate the full ingest-and-ask
flow against a real PostgreSQL/PGVector instance.

## Deployment

The application and its database can both be run as containers:

```bash
docker compose up --build
```

`docker-compose.yml` defines two services: `app` (the Spring Boot application)
and `postgres` (PostgreSQL with the PGVector extension pre-installed).

## Roadmap

| Feature | Status |
|---|---|
| Core RAG pipeline (ingest, embed, retrieve, generate) | Complete |
| REST API | Complete |
| JWT authentication and role-based access | Complete |
| Multi-file ingestion | Planned |
| Redis-backed response caching | Planned |
| Rate limiting | Planned |
| Minimal web frontend | Planned |

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
