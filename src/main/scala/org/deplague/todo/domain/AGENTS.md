# Domain Layer

## Rules

- **ZERO framework dependencies**. No ZIO, no JSON, no HTTP, no database libraries.
- Pure Scala + JDK only (`java.time`, `java.util.UUID`, etc.).

## Patterns

### Value Objects

Use `final case class` with **private constructor** and factory method:

```scala
final case class Title private(value: String)

object Title:
  def create(value: String): Either[DomainError.ValidationError, Title] =
    // validation
```

- Validate in the factory method.
- Return `Either[DomainError.ValidationError, X]` — never throw exceptions.

### Entities

Entities contain business logic and invariants:

```scala
final case class Todo(...):
  def complete: Either[DomainError.InvalidStateTransition, Todo] = ...
```

### Errors

Use the shared `DomainError` enum:
- `ValidationError(message)` — input validation failures
- `TodoNotFound(id)` — repository miss
- `InvalidStateTransition(message)` — business rule violation

### Testing

Test with plain Scala assertions. No ZIO Test needed for domain logic (though we use ZIO Test for uniformity).
