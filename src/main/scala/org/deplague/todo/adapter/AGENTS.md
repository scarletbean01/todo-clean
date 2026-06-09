# Adapter Layer

## Rules

- This layer **may use ZIO, ZIO HTTP, ZIO JSON**, and any infrastructure libraries.
- Depends inward on application ports and domain errors only.

## Structure

### Incoming Adapters (`in/web/`)

- **Controllers/Routes**: ZIO HTTP `Routes` that map HTTP to use case calls.
- **Request DTOs** (`request/`): JSON-decodable case classes. Named `XxxRequest`.
- **Response DTOs** (`response/`): JSON-encodable case classes. Named `XxxDto`.

### Responsibilities

1. Parse HTTP request body → `XxxRequest`
2. Map `XxxRequest` → `XxxCommand` (application DTO)
3. Call use case through incoming port
4. Map result to HTTP response:
   - `Right(response)` → 200 with JSON
   - `Left(ValidationError)` → 400 Bad Request
   - `Left(TodoNotFound)` → 404 Not Found
   - `Left(InvalidStateTransition)` → 400 Bad Request

### Outgoing Adapters (`out/persistence/`)

- Implement outgoing ports (e.g., `SaveTodoPort`, `LoadTodoPort`).
- **In-memory adapter**: Uses `ConcurrentHashMap` for thread safety.
- Pure interface, effectful implementation internally.
- Companion object exposes a `live` ZLayer factory:

```scala
object InMemoryTodoRepository:
  val live: ZLayer[Any, Nothing, SaveTodoPort & LoadTodoPort & FindTodosPort] =
    ZLayer.succeed(new InMemoryTodoRepository())
```

- Comment or stub where a production adapter (e.g., Doobie) would plug in.

## Mapping

```
HTTP Request  →  Web DTO      →  Application Command  →  Domain Entity
HTTP Response ←  Web DTO      ←  Application Response  ←  Domain Entity
```
