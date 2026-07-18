# OmniSentry

OmniSentry is a modular personal finance backend focused on bank-account aggregation, transaction storage, authentication, analytics, and AI-assisted financial insights.

The system is split into small services:

- `os-gateway` exposes a single HTTP entry point and validates JWT cookies.
- `os-authenticator` handles registration, login, password hashing, JWT signing, and JWKS publication.
- `os-main-backend` stores users, bank connections, accounts, and transactions, and integrates with Salt Edge.
- `os-analyser` runs Scala-based transaction analytics and exposes them over gRPC.
- `os-app-agent` exposes an AI chat endpoint backed by Koog, Ollama, and analytics tools.
- `os-shared-core` contains shared DTOs and Protocol Buffer definitions used by the JVM services.

## Architecture

```text
Client
  |
  v
os-gateway :8080
  |-- /api/auth/**    -> os-authenticator :8081
  |-- /api/backend/** -> os-main-backend :8082
  |-- /api/ai/**      -> os-app-agent :8085

os-authenticator :8081
  |-- publishes JWKS at /.well-known/jwks.json
  |-- calls os-main-backend over gRPC for user registration

os-main-backend :8082
  |-- exposes business HTTP endpoints
  |-- exposes gRPC services on :9092
  |-- stores data in PostgreSQL
  |-- calls Salt Edge API

os-analyser :9093
  |-- calls os-main-backend gRPC transaction history service
  |-- returns expense and anomaly analysis

os-app-agent :8085
  |-- calls os-analyser over gRPC
  |-- calls local Ollama for AI responses
```

## Module Overview

### `os-gateway`

Spring Cloud Gateway MVC service written in Kotlin. It validates the `OmniSentryJwt` HTTP-only cookie as a JWT, loads public keys from the authenticator JWKS endpoint, and forwards authenticated requests to downstream services. For authenticated requests it adds `X-User-Username` from the JWT subject/name context.

Main routes:

- `/api/auth/**` -> authenticator, public
- `/api/backend/**` -> main backend, authenticated except Salt Edge callbacks
- `/api/ai/**` -> app agent, authenticated

### `os-authenticator`

Spring Boot Kotlin service responsible for:

- user registration and login
- BCrypt password hashing
- RSA-signed JWT generation
- publishing public JWKS at `/.well-known/jwks.json`
- calling `UserRegistrationService` in `os-main-backend` over gRPC during registration

HTTP endpoints:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/check-existence`
- `GET /.well-known/jwks.json`

### `os-main-backend`

Spring Boot Java service responsible for core finance data and Salt Edge integration.

Responsibilities:

- creating Salt Edge customers during user registration
- creating Salt Edge connect sessions
- storing users, connections, accounts, and transactions
- handling Salt Edge callbacks
- serving transaction history to analytics over gRPC

HTTP endpoints include:

- `GET /user`
- `GET /user/transactions?connection_id=...`
- `POST /user/connection/register`
- `GET /user/connections`
- `GET /user/accounts?connection_id=...`
- `POST /callbacks/saltedge/success`
- `POST /callbacks/saltedge/fail`
- `POST /callbacks/saltedge/destroy`
- `POST /callbacks/saltedge/notify`
- `POST /callbacks/saltedge/changes/provider`
- `POST /callbacks/saltedge/changes/consent`

gRPC services:

- `UserRegistrationService`
- `AnalyticsDataService`

### `os-analyser`

Scala 3 service built with SBT. It exposes `AnalysisService` over gRPC on port `9093`.

Analytics implemented today:

- expense analysis grouped by currency and transaction category
- anomaly analysis using Smile `IsolationForest`

The analyser fetches transaction history from `os-main-backend` through the `AnalyticsDataService` gRPC API.

### `os-app-agent`

Ktor/Kotlin service using Koog agents. It exposes a chat endpoint and gives the LLM tools for financial analysis.

Current endpoint:

- `POST /message`

Expected request body:

```json
{
  "customerId": "salt-edge-customer-id",
  "connectionId": "salt-edge-connection-id",
  "message": "Analyze my spending"
}
```

The default implementation uses Ollama with `qwen3:8b` and calls the analyser over gRPC through:

- `ExpensesTool`
- `AnomalyTool`

### `os-shared-core`

Shared Kotlin module containing DTOs and `.proto` files for cross-service communication:

- `UserRegistration.proto`
- `transactions.proto`
- `analysis.proto`

## Technology Stack

- Kotlin 2.x
- Java 21
- Scala 3.3
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway MVC
- Spring gRPC
- Ktor 3
- Koog agents
- Ollama
- gRPC and Protocol Buffers
- PostgreSQL
- MongoDB
- Docker Compose
- Gradle 9
- SBT

## Ports

| Service | HTTP | gRPC | Notes |
| --- | ---: | ---: | --- |
| `os-gateway` | `8080` | - | Public entry point |
| `os-authenticator` | `8081` | - | Auth and JWKS |
| `os-main-backend` | `8082` | `9092` | Core finance backend |
| `os-analyser` | - | `9093` | Scala analytics service |
| `os-app-agent` | `8085` | - | AI chat service |

## Environment Variables

Create a `.env` file in the repository root when running with Docker Compose.

```dotenv
# Salt Edge
SALT_EDGE_URL=saltedge url with version
SALT_EDGE_APP_ID=your_app_id
SALT_EDGE_API_SECRET=your_secret

#Check docker-compose.yml for default values

# Main backend database
MAIN_DB_NAME=main_db
MAIN_DB_HOST=os-main-backend-database:5432
MAIN_DB_USER=admin
MAIN_DB_PASSWORD=admin

# Gateway
RESOURCE_SERVER_BASE_URL=http://os-authenticator:8081

# Authenticator database
AUTH_DB_NAME=auth_db
AUTH_DB_HOST=os-authenticator-database:5432
AUTH_DB_USER=admin
AUTH_DB_PASSWORD=admin

# Authenticator RSA keys
OMNISENTRY_RSA_PUBLIC_KEY=base64_public_key
OMNISENTRY_RSA_PRIVATE_KEY=base64_private_key

AGENT_TYPE=GROQ #else will inject ollama
GROQ_API_KEY=groq_api_key

AGENT_DB_USER=admin
AGENT_DB_PASSWORD=admin

# os-analyser service host and port
GRPC_HOST=os-analyser
GRPC_PORT=9093
```

Generate RSA key material with:

```bash
./KeyGen.main.kts
```

If the script is not executable:

```bash
kotlin KeyGen.main.kts
```

## Running With Docker Compose

```bash
docker compose up --build
```

Gateway will be available at:

```text
http://localhost:8080
```

## Running Locally

Build all Gradle modules:

```bash
./gradlew build
```

Run individual Gradle services:

```bash
./gradlew :os-gateway:bootRun
./gradlew :os-authenticator:bootRun
./gradlew :os-main-backend:bootRun
./gradlew :os-app-agent:run
```

Run the Scala analyser:

```bash
cd os-analyser
sbt run
```

For the AI agent, Ollama must be running locally and the configured model must be available:

```bash
ollama pull llama3.1:8b-instruct-q4_K_M
ollama serve
```

## API Examples

Register a user through the gateway:

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "testuser1",
    "name": "Test",
    "surname": "User",
    "dateOfBirth": "1990-01-01T00:00:00Z",
    "email": "test@example.com",
    "pass": "password123"
  }'
```

Log in:

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "testuser1",
    "pass": "password123"
  }'
```

Call the AI agent directly:

```bash
curl -X POST http://localhost:8085/message \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "customer-id",
    "connectionId": "connection-id",
    "message": "Summarize my expenses and check for anomalies"
  }'
```

## Development Notes

- `os-analyser` is not included in `settings.gradle.kts`; it is an independent SBT project.
- Some internal hostnames in code and Docker Compose should be kept aligned before full Compose deployment. For example, gateway routes currently reference service names such as `os-backend` and `os-agent`, while Compose defines `os-main-backend` and `os-app-agent`.
- `os-app-agent` depends on Koin/Ktor integration when using `install(Koin)`. Make sure `io.insert-koin:koin-ktor` is present and `org.koin.ktor.plugin.Koin` is imported.
- The main backend Dockerfile uses an `eclipse-temurin:21-jre` runtime image but runs an Alpine `apk` command. Use an Alpine-based runtime image or remove that command if the image build fails.
- The Salt Edge configuration logs credentials during startup. Avoid logging secrets in shared or production environments.

## Repository Layout

```text
.
|-- os-gateway/         Spring Cloud Gateway MVC service
|-- os-authenticator/   Authentication and JWT service
|-- os-main-backend/    Core finance backend and Salt Edge integration
|-- os-analyser/        Scala gRPC analytics service
|-- os-app-agent/       Ktor and Koog AI agent service
|-- os-shared-core/     Shared DTOs and proto definitions
|-- docker-compose.yml
|-- build.gradle.kts
|-- settings.gradle.kts
`-- KeyGen.main.kts
```
