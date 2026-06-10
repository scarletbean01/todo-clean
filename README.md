# Todo Manager — Clean Architecture + Tagless Final with ZIO

A demo REST API for managing todos, built with **Scala 3**, **ZIO**, and **ZIO HTTP** following **Clean Architecture** (Hexagonal Architecture) principles, enhanced with a lightweight **Tagless Final** abstraction via a custom `Effect[F[_]]` typeclass.

The domain layer has **zero framework dependencies**. The application layer is **polymorphic in `F[_]`**, allowing the core to be tested synchronously with `Either` while adapters run on ZIO.

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

**Response 400 Bad Request** — empty title, title too long, or description too long.

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

This project combines **Clean Architecture** (ports and adapters) with a lightweight **Tagless Final** abstraction.

### Dependency Rule

Dependencies point **inward**. The domain knows nothing about ZIO, HTTP, JSON, or databases. The application layer is polymorphic in `F[_]` — it knows about effects abstractly, but not about ZIO concretely.

```
┌─────────────────────────────────────────────┐
│  Bootstrap (ZLayer wiring, commits to TaskE) │
├─────────────────────────────────────────────┤
│  Adapter (ZIO HTTP, ZIO JSON, TaskE alias)   │
│  → provides given Effect[TaskE]              │
├─────────────────────────────────────────────┤
│  Application (polymorphic in F[_])           │
│  → Effect[F[_]] typeclass                    │
│  → ports: SaveTodoPort[F], LoadTodoPort[F]   │
│  → services: CreateTodoService[F]            │
├─────────────────────────────────────────────┤
│  Domain (pure, zero deps, returns Either)    │
│  → Title.create: Either[ValidationError, _]  │
│  → Todo.complete: Either[InvalidState, _]    │
└─────────────────────────────────────────────┘
```

### The `Effect[F[_]]` Typeclass

A minimal abstraction over effect systems (no Cats dependency):

```scala
trait Effect[F[_]]:
  def pure[A](a: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def fromEither[A](ea: Either[DomainError, A]): F[A]
  def raiseError[A](e: DomainError): F[A]
```

The application layer uses `Effect.fromEither(...)` to lift pure domain results into the abstract effect:

```scala
class CreateTodoService[F[_]: Effect](savePort: SaveTodoPort[F]) extends CreateTodoUseCase[F]:
  def create(command: CreateTodoCommand): F[TodoResponse] =
    for
      title       <- Effect.fromEither(Title.create(command.title))
      description <- Effect.fromEither(Description.create(...))
      todo = Todo(...)
      _           <- savePort.save(todo)
    yield TodoResponse.fromDomain(todo)
```

### Concrete Effect: `TaskE`

The adapter layer defines the concrete effect and its `Effect` instance:

```scala
type TaskE[A] = ZIO[Any, DomainError, A]

given Effect[TaskE] with
  def pure[A](a: A)           = ZIO.succeed(a)
  def fromEither[A](ea)       = ZIO.fromEither(ea)
  def raiseError[A](e)        = ZIO.fail(e)
  ...
```

### Layer Breakdown

#### Domain (`todo/domain/`)

Pure Scala. No imports from `zio.*`, `zio.http.*`, or any framework.

- **Entities**: `Todo` — contains business rules (e.g., cannot complete an already-completed todo).
- **Value Objects**: `TodoId`, `Title`, `Description` — validated at construction.
- **Errors**: `DomainError` — sealed enum for `ValidationError`, `TodoNotFound`, `InvalidStateTransition`.

#### Application (`todo/application/`)

Orchestrates domain logic. Polymorphic in `F[_]` via `Effect`. No ZIO, HTTP, or JSON.

- **Effect** (`Effect.scala`): The `Effect[F[_]]` typeclass and syntax.
- **Incoming Ports** (`port/in/`): `CreateTodoUseCase[F]`, `CompleteTodoUseCase[F]`, etc.
- **Outgoing Ports** (`port/out/`): `SaveTodoPort[F]`, `LoadTodoPort[F]`, `FindTodosPort[F]` — narrow, ISP-compliant.
- **Services** (`service/`): Implement incoming ports, returning `F[A]`.
- **DTOs** (`dto/`): Dedicated command/response models per use case.

#### Adapter (`todo/adapter/`)

Bridges the application to the outside world. Provides the concrete `Effect[TaskE]`.

- **Web Adapter** (`adapter/in/web/`): ZIO HTTP routes. Parses JSON, maps to commands, calls use cases, handles errors via ZIO's `catchAll`.
- **Persistence Adapter** (`adapter/out/persistence/`): In-memory `ConcurrentHashMap` implementation. Exposes a `ZLayer` factory for wiring.

#### Bootstrap (`todo/bootstrap/`)

Wires everything together via **ZLayer composition**:

```scala
object AppLayer:
  val live: ZLayer[Any, Nothing, Routes[Any, Response]] =
    InMemoryTodoRepository.live >+>
      ZLayer.fromFunction((save: SaveTodoPort[TaskE]) =>
        new CreateTodoService[TaskE](save)
      ) >+>
      ... >+>
      TodoRoutes.live
```

---

## Why Tagless Final Here?

Clean Architecture's ports abstract over **infrastructure** (DB, HTTP). Tagless final (`Effect[F[_]]`) abstracts over **effect systems** (ZIO, Cats Effect, or synchronous `Either`).

By combining both:

1. **Domain stays pure** — business logic is `Either`-based, testable with plain assertions.
2. **Application is effect-agnostic** — use cases work with any `F[_]` that has an `Effect` instance.
3. **Adapters commit to ZIO** — the web server and repository run on `TaskE`.
4. **Tests run synchronously** — provide `Effect[Either[DomainError, *]]` and test use cases without a ZIO runtime.

To swap the effect runtime (e.g., ZIO → Cats Effect): provide a new `Effect[IO]` instance and update the bootstrap. Domain and application services remain unchanged.

---

## Testing Strategy

| Layer | Test Type | Approach |
|-------|-----------|----------|
| Domain | Unit | Plain Scala assertions. No mocking. No ZIO runtime. |
| Use Cases | Unit | Provide `Effect[Either[DomainError, *]]`. Mock ports with simple fakes. Tests run synchronously. |
| Adapters | Integration | (Not included in demo) Test against real ZIO HTTP / Testcontainers. |

Example — testing use cases without ZIO:

```scala
type IdEither[A] = Either[DomainError, A]

given Effect[IdEither] with
  def pure[A](a: A) = Right(a)
  def fromEither[A](ea) = ea
  ...

val service = new CreateTodoService[IdEither](fakePort)
val result = service.create(command) // Just Either!
```

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
    │   │   ├── Effect.scala           # Effect[F[_]] typeclass
    │   │   ├── port/
    │   │   │   ├── in/                # Incoming ports (use cases)
    │   │   │   └── out/               # Outgoing ports (repository interfaces)
    │   │   ├── dto/                   # Commands and responses
    │   │   └── service/               # Use case implementations
    │   ├── adapter/
    │   │   ├── AGENTS.md              # Adapter layer conventions
    │   │   ├── TaskE.scala            # Concrete TaskE = ZIO alias + instance
    │   │   ├── in/web/                # ZIO HTTP routes, JSON DTOs
    │   │   └── out/persistence/       # In-memory repository (ZLayer)
    │   └── bootstrap/
    │       ├── AGENTS.md              # Bootstrap/wiring conventions
    │       ├── AppLayer.scala         # ZLayer composition
    │       └── MainApp.scala          # Entry point
    └── test/scala/org/deplague/todo/
        ├── domain/                    # Domain unit tests
        └── application/service/       # Use case unit tests (Either-based)
```

---

## License

MIT
