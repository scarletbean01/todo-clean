# Bootstrap Layer

## Rules

- The only layer allowed to instantiate and wire all components.
- Has access to all layers (domain, application, adapter).
- Uses ZIO runtime and `ZLayer` for dependency injection.

## Patterns

### ZLayer Composition

Each adapter and application service exposes a `live` ZLayer factory. The bootstrap composes them declaratively:

```scala
val appLayer =
  InMemoryTodoRepository.live >+>
    CreateTodoService.live >+>
    CompleteTodoService.live >+>
    GetTodoService.live >+>
    ListTodosService.live >+>
    TodoRoutes.live

ZIO.service[Routes[Any, Response]]
  .flatMap(Server.serve)
  .provide(Server.default, appLayer)
```

Domain and application service logic remain pure (`Either`-based); only construction is lifted into ZIO.

## Extending

To swap in a Postgres adapter:
1. Implement `SaveTodoPort`, `LoadTodoPort`, `FindTodosPort` with Doobie.
2. Replace `InMemoryTodoRepository.live` with `PostgresTodoRepository.live`.
3. No changes to domain, use cases, or web routes.
