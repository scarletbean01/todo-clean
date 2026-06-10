# Adapter Layer

## Rules

- This layer **may use ZIO, ZIO HTTP, ZIO JSON**, and any infrastructure libraries.
- Depends inward on application ports and domain errors only.
- Provides the **concrete** `Effect[F]` instance for ZIO.

## Structure

### Incoming Adapters (`in/web/`)

- **Controllers/Routes**: ZIO HTTP `Routes` that map HTTP to use case calls.
- **Request DTOs** (`request/`): JSON-decodable case classes. Named `XxxRequest`.
- **Response DTOs** (`response/`): JSON-encodable case classes. Named `XxxDto`.

### Responsibilities

1. Parse HTTP request body → `XxxRequest`
2. Map `XxxRequest` → `XxxCommand` (application DTO)
3. Call use case through incoming port
4. Handle effectful results directly in ZIO:

```scala
createUseCase.create(command).map { response =>
  Response.json(TodoDto.fromResponse(response).toJson)
}.catchAll { error =>
  ZIO.succeed(mapDomainError(error))
}
```

### Concrete Effect Instance (`TaskE`)

The adapter package defines the concrete effect type and its `Effect` instance:

```scala
type TaskE[A] = ZIO[Any, DomainError, A]

given Effect[TaskE] with
  def pure[A](a: A) = ZIO.succeed(a)
  def fromEither[A](ea) = ZIO.fromEither(ea)
  def raiseError[A](e) = ZIO.fail(e)
  ...
```

### Outgoing Adapters (`out/persistence/`)

- Implement outgoing ports (e.g., `SaveTodoPort[TaskE]`, `LoadTodoPort[TaskE]`).
- **In-memory adapter**: Uses `ConcurrentHashMap` for thread safety.
- Pure interface, effectful implementation internally.
- Companion object exposes a concrete `live` ZLayer factory:

```scala
object InMemoryTodoRepository:
  val live: ZLayer[Any, Nothing, SaveTodoPort[TaskE] & LoadTodoPort[TaskE] & FindTodosPort[TaskE]] =
    ZLayer.succeed(new InMemoryTodoRepository[TaskE])
```

- Comment or stub where a production adapter (e.g., Doobie) would plug in.

## Mapping

```
HTTP Request  →  Web DTO      →  Application Command  →  Domain Entity
HTTP Response ←  Web DTO      ←  Application Response  ←  Domain Entity
```
