# Application Layer

## Rules

- No framework dependencies except Scala standard library.
- May reference `DomainError` and domain entities.
- **No ZIO, no HTTP, no JSON**.

## Structure

### Ports (`port/`)

- **Incoming ports** (`port/in/`): Interfaces implemented by use cases. Named `XxxUseCase`.
- **Outgoing ports** (`port/out/`): Interfaces implemented by adapters. Named after capability, e.g., `SaveTodoPort`, `LoadTodoPort`.
- Apply **ISP**: keep ports narrow. One method per port when possible.

### DTOs (`dto/`)

- **Dedicated per use case**. Never share between use cases.
- Commands are raw data: `case class CreateTodoCommand(title: String, description: Option[String])`
- Responses decouple domain from outer layers: `case class TodoResponse(...)`

### Services (`service/`)

- Named `XxxService`, implements `XxxUseCase`.
- Orchestrate ports and domain logic.
- Return `Either[DomainError, A]` — service logic never uses `ZIO`.
- Constructor injection of ports via plain parameters.
- Companion object exposes a `live` ZLayer factory for bootstrap wiring:

```scala
class CreateTodoService(savePort: SaveTodoPort) extends CreateTodoUseCase:
  def create(command: CreateTodoCommand): Either[DomainError, TodoResponse] = ...

object CreateTodoService:
  val live: ZLayer[SaveTodoPort, Nothing, CreateTodoUseCase] =
    ZLayer.fromFunction(new CreateTodoService(_))
```

### Testing

Mock outgoing ports with simple in-memory fakes. Verify:
1. Correct port interactions
2. Domain logic application
3. Error propagation
