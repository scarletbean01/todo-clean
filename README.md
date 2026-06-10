# Todo Manager — Clean Architecture with ZIO

A demo REST API for managing todos, built with **Scala 3**, **ZIO**, and **ZIO HTTP** following **Clean Architecture** (aka Hexagonal Architecture) principles.

The domain layer has **zero framework dependencies**. All business logic is expressed in pure Scala, tested in isolation, and protected from infrastructure concerns by ports and adapters.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Scala 3.8.4 |
| Effect System | ZIO 2.1 |
| Web Server | ZIO HTTP 3.0 |
| JSON | ZIO JSON |
| Testing | ZIO Test |
| Build Tool | sbt |

---

## Quick Start

### Prerequisites

- JDK 21+
- sbt

### Build & Test

```bash
# Compile
sbt compile

# Run all tests
sbt test
```

### Run the Server

```bash
sbt run
```

The HTTP server starts on **port 8080**.

---

## API Reference

### Create a Todo

```bash
POST /todos
Content-Type: application/json

{
  "title": "Buy groceries",
  "description": "Milk, eggs, bread"
}
```

**Response 200 OK**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Buy groceries",
  "description": "Milk, eggs, bread",
  "status": "Pending",
  "createdAt": "2026-06-09T14:00:00Z"
}
```

**Response 400 Bad Request** (empty title, title too long, or description too long)

---

### Complete a Todo

```bash
POST /todos/{id}/complete
```

**Response 200 OK** — returns the completed todo.

**Response 400 Bad Request** — todo is already completed.

**Response 404 Not Found** — todo does not exist.

---

### Get a Todo

```bash
GET /todos/{id}
```

**Response 200 OK**

**Response 404 Not Found** — todo does not exist.

---

### List All Todos

```bash
GET /todos
```

**Response 200 OK** — array of todos.

---

## Architecture

This project follows **Clean Architecture** / **Hexagonal Architecture**.

### Dependency Rule

Dependencies point **inward**. The domain knows nothing about ZIO, HTTP, JSON, or databases.

```
┌─────────────────────────────────────┐
│  Web Adapter (ZIO HTTP + JSON)      │  ← Frameworks
├─────────────────────────────────────┤
│  Use Cases + Ports                  │  ← Application
├─────────────────────────────────────┤
│  Domain (Entities + Value Objects)  │  ← Pure business logic
└─────────────────────────────────────┘
```

### Layer Breakdown

#### Domain (`todo/domain/`)

Pure Scala. No imports from `zio.*`, `zio.http.*`, or any framework.

- **Entities**: `Todo` — contains business rules (e.g., cannot complete an already-completed todo).
- **Value Objects**: `TodoId`, `Title`, `Description` — validated at construction.
- **Errors**: `DomainError` — sealed enum for `ValidationError`, `TodoNotFound`, `InvalidStateTransition`.

#### Application (`todo/application/`)

Orchestrates domain logic. Still no framework dependencies.

- **Incoming Ports** (`port/in/`): `CreateTodoUseCase`, `CompleteTodoUseCase`, `GetTodoUseCase`, `ListTodosUseCase`
- **Outgoing Ports** (`port/out/`): `SaveTodoPort`, `LoadTodoPort`, `FindTodosPort` — narrow, single-method interfaces.
- **Services** (`service/`): Implement incoming ports, returning `Either[DomainError, A]`.
- **DTOs** (`dto/`): Dedicated command/response models per use case.

#### Adapter (`todo/adapter/`)

Bridges the application to the outside world.

- **Web Adapter** (`adapter/in/web/`): ZIO HTTP routes. Parses JSON, maps to commands, calls use cases, maps errors to HTTP status codes.
- **Persistence Adapter** (`adapter/out/persistence/`): In-memory `ConcurrentHashMap` implementation of the outgoing ports. Swappable for a real database adapter (e.g., Doobie) without touching domain or application code.

#### Bootstrap (`todo/bootstrap/`)

Wires everything together manually:

```scala
val repository = new InMemoryTodoRepository()
val createService = new CreateTodoService(repository)
val routes = TodoRoutes(createService, ...)
Server.serve(routes).provide(Server.default)
```

---

## Why Not Tagless Final?

Tagless final (`F[_]`) abstracts over **effect systems** (e.g., ZIO vs. Cats Effect). Clean Architecture's **ports and adapters** already abstract over **infrastructure** (e.g., in-memory vs. Postgres, HTTP vs. CLI).

Since this project commits to ZIO as its runtime, adding tagless final would introduce indirection without value. The domain is fully decoupled from ZIO through ports — if you ever needed to migrate from ZIO to Cats Effect, only the `adapter.in.web` and `bootstrap` packages would change.

---

## Testing Strategy

| Layer | Test Type | Approach |
|-------|-----------|----------|
| Domain | Unit | Plain Scala assertions. No mocking. |
| Use Cases | Unit | Mock outgoing ports with simple fakes. Verify orchestration and error handling. |
| Adapters | Integration | (Not included in demo) Test against real ZIO HTTP / Testcontainers. |

Run tests:

```bash
sbt test
```

---

## Project Structure

```
.
├── AGENTS.md                          # Agent guidance (root)
├── README.md                          # This file
├── build.sbt                          # Build definition
└── src/
    ├── main/scala/org/deplague/todo/
    │   ├── domain/
    │   │   ├── AGENTS.md              # Domain layer conventions
    │   │   ├── DomainError.scala
    │   │   ├── Todo.scala
    │   │   ├── TodoId.scala
    │   │   ├── Title.scala
    │   │   ├── Description.scala
    │   │   └── Status.scala
    │   ├── application/
    │   │   ├── AGENTS.md              # Application layer conventions
    │   │   ├── port/
    │   │   │   ├── in/                # Incoming ports (use cases)
    │   │   │   └── out/               # Outgoing ports (repository interfaces)
    │   │   ├── dto/                   # Commands and responses
    │   │   └── service/               # Use case implementations
    │   ├── adapter/
    │   │   ├── AGENTS.md              # Adapter layer conventions
    │   │   ├── in/web/                # ZIO HTTP routes, JSON DTOs
    │   │   └── out/persistence/       # In-memory repository
    │   └── bootstrap/
    │       ├── AGENTS.md              # Bootstrap/wiring conventions
    │       └── MainApp.scala          # Entry point
    └── test/scala/org/deplague/todo/
        ├── domain/                    # Domain unit tests
        └── application/service/       # Use case unit tests
```

---

## License

MIT
