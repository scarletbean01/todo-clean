# Application Layer

## Rules

- No framework dependencies except Scala standard library.
- May reference `DomainError` and domain entities.
- **No ZIO, no HTTP, no JSON**.
- This layer is **polymorphic** in `F[_]` via the `Effect` typeclass.

## The `Effect` Typeclass

A minimal abstraction over effect systems, defined in this layer:

```scala
trait Effect[F[_]]:
  def pure[A](a: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def fromEither[A](ea: Either[DomainError, A]): F[A]
  def raiseError[A](e: DomainError): F[A]
```

No Cats dependency. The application layer uses `Effect` to orchestrate domain logic and ports without committing to an effect system.

Extension methods on `F[A]` (given `Effect[F]`) enable `for` comprehensions. When `F` is concrete (e.g., `ZIO`, `Either`), the concrete type's own `map`/`flatMap` take precedence.

## Structure

### Ports (`port/`)

- **Incoming ports** (`port/in/`): Interfaces implemented by use cases. Named `XxxUseCase`. Parameterized by `F[_]`.
- **Outgoing ports** (`port/out/`): Interfaces implemented by adapters. Named after capability, e.g., `SaveTodoPort`, `LoadTodoPort`. Parameterized by `F[_]`.
- Apply **ISP**: keep ports narrow. One method per port when possible.

```scala
trait SaveTodoPort[F[_]]:
  def save(todo: Todo): F[Unit]
```

### DTOs (`dto/`)

- **Dedicated per use case**. Never share between use cases.
- Commands are raw data: `case class CreateTodoCommand(title: String, description: Option[String])`
- Responses decouple domain from outer layers: `case class TodoResponse(...)`

### Services (`service/`)

- Named `XxxService`, implements `XxxUseCase[F]`.
- Orchestrate ports and domain logic.
- Return `F[A]` — abstract effect, never a concrete framework type.
- Constructor injection of ports via plain parameters.
- Use `Effect.fromEither(...)` to lift domain `Either` results into `F`:

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

**No ZLayer factories here.** Application services are plain classes. Concrete construction happens in the bootstrap layer.

### Testing

The killer feature of tagless final: test **synchronously** with `Either[DomainError, *]` — no ZIO runtime needed.

```scala
type IdEither[A] = Either[DomainError, A]

given Effect[IdEither] with
  def pure[A](a: A) = Right(a)
  def fromEither[A](ea) = ea
  ...

val service = new CreateTodoService[IdEither](fakePort)
val result = service.create(command) // Just Either!
```

Mock outgoing ports with simple in-memory fakes. Verify:
1. Correct port interactions
2. Domain logic application
3. Error propagation
