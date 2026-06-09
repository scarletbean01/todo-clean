# Project: untitled1 — Todo Manager (Clean Architecture + ZIO)

## Overview

This is a demo project implementing a Todo Manager REST API using:
- **Scala 3.8.4**
- **ZIO 2.1** (effect system and runtime)
- **ZIO HTTP 3.0** (web server)
- **ZIO JSON** (JSON serialization)
- **Strict Clean Architecture** (aka Hexagonal Architecture)

The architecture follows the Dependency Rule: domain code has zero framework dependencies. Dependencies point inward.

## Build & Run

```bash
# Compile
sbt compile

# Run tests
sbt test

# Run the HTTP server
sbt run
```

Server starts on default port (8080).

## API Endpoints

| Method | Path | Use Case |
|--------|------|----------|
| POST | /todos | CreateTodo |
| POST | /todos/:id/complete | CompleteTodo |
| GET | /todos/:id | GetTodo |
| GET | /todos | ListTodos |

## Architecture Principles

1. **Dependency Rule**: All dependencies point inward. Domain has zero framework dependencies.
2. **Ports and Adapters**: Domain defines interfaces (ports); infrastructure implements them (adapters).
3. **Narrow Ports**: Apply ISP. One method per port when possible.
4. **Dedicated DTOs**: Each use case has its own input command and output model.
5. **Full Mapping**: Web DTOs → Application DTOs → Domain Entities.
6. **Effect Boundary**: Domain and use cases return `Either[DomainError, A]`. Adapters lift into ZIO.

## Layer Conventions

See `AGENTS.md` files in each layer directory for specific conventions.

## ZLayer Dependency Injection

The bootstrap layer uses **ZLayer** for dependency injection. Each adapter and application service exposes a `live` ZLayer factory in its companion object. This keeps wiring declarative and composable while preserving the clean architecture boundary — domain and application service logic remain pure (`Either`-based), and only construction is lifted into ZIO.

## Why No Tagless Final

Tagless final (`F[_]`) is designed to abstract over *effect systems*. Clean Architecture's ports already abstract over *infrastructure* (DB, HTTP, external APIs). Since this project commits to ZIO as its runtime, adding tagless final would create redundant abstraction without value. The domain is already decoupled from ZIO through the port/adapter pattern.
