# Bootstrap Layer

## Rules

- The only layer allowed to instantiate and wire all components.
- Has access to all layers (domain, application, adapter).
- Uses ZIO runtime and `ZLayer` for dependency injection.
- Commits to the concrete effect type `TaskE = ZIO[Any, DomainError, *]`.

## Patterns

### ZLayer Composition

Application services are **plain polymorphic classes** (`F[_]`). The bootstrap constructs them with the concrete `TaskE` type inside ZLayer definitions:

```scala
import org.deplague.todo.adapter.{TaskE, given}

val live: ZLayer[Any, Nothing, Routes[Any, Response]] =
  InMemoryTodoRepository.live >+>
    ZLayer.fromFunction((save: SaveTodoPort[TaskE]) => new CreateTodoService[TaskE](save)) >+>
    ZLayer.fromZIO {
      for {
        load <- ZIO.service[LoadTodoPort[TaskE]]
        save <- ZIO.service[SaveTodoPort[TaskE]]
      } yield new CompleteTodoService[TaskE](load, save)
    } >+>
    ZLayer.fromFunction((load: LoadTodoPort[TaskE]) => new GetTodoService[TaskE](load)) >+>
    ZLayer.fromFunction((find: FindTodosPort[TaskE]) => new ListTodosService[TaskE](find)) >+>
    TodoRoutes.live
```

Domain and application service logic remain abstract (`F[_]`); only the bootstrap commits to ZIO.

## Extending

To swap in a Postgres adapter:
1. Implement `SaveTodoPort[TaskE]`, `LoadTodoPort[TaskE]`, `FindTodosPort[TaskE]` with Doobie.
2. Replace `InMemoryTodoRepository.live` with `PostgresTodoRepository.live`.
3. No changes to domain, use cases, or web routes.

To swap the effect runtime (e.g., ZIO → Cats Effect):
1. Provide a new `Effect[IO]` instance in the adapter layer.
2. Update `TaskE` alias and bootstrap construction.
3. No changes to domain or application services.
