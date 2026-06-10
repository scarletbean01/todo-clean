# Project: untitled1 — Todo Manager (Clean Architecture + Tagless Final + ZIO)

## Overview

This is a demo project implementing a Todo Manager REST API using:
- **Scala 3.8.4**
- **Tagless Final** (`F[_]` abstraction via a lightweight custom `Effect` typeclass)
- **ZIO 2.1** (concrete effect runtime)
- **ZIO HTTP 3.0** (web server)
- **ZIO JSON** (JSON serialization)
- **Strict Clean Architecture** (aka Hexagonal Architecture)

The architecture follows the Dependency Rule: domain code has zero framework dependencies. Dependencies point inward. The application layer is **polymorphic** in `F[_]` via a lightweight custom `Effect` typeclass, allowing the core to be tested synchronously with `Either` while adapters run on ZIO.

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
6. **Effect Boundary**:
   - **Domain** returns `Either[DomainError, A]` — pure, no effects.
   - **Application** is polymorphic in `F[_]`: ports and services return `F[A]`.
   - **Adapter** provides the concrete `Effect[F]` instance (ZIO) and lifts `Either` into `F`.
   - **Bootstrap** wires the concrete effect type (`TaskE = ZIO[Any, DomainError, *]`).

## Layer Conventions

See `AGENTS.md` files in each layer directory for specific conventions.

## Tagless Final + ZLayer

The application layer uses a **custom `Effect[F[_]]` typeclass** (no Cats dependency) to abstract over effect systems:

```scala
trait Effect[F[_]]:
  def pure[A](a: A): F[A]
  def fromEither[A](ea: Either[DomainError, A]): F[A]
  def raiseError[A](e: DomainError): F[A]
  ...
```

Domain results are lifted via `Effect.fromEither(...)` inside `for` comprehensions. The adapter layer provides a `given Effect[TaskE]` for ZIO. Tests provide a `given Effect[Either[DomainError, *]]` for synchronous, deterministic execution.

ZLayer is used in the **adapter** and **bootstrap** layers only. Application services are plain polymorphic classes; concrete construction happens in bootstrap.
