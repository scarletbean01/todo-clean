package org.deplague.todo.application.service

import org.deplague.todo.application.dto.{CreateTodoCommand, TodoResponse}
import org.deplague.todo.application.port.in.CreateTodoUseCase
import org.deplague.todo.application.port.out.SaveTodoPort
import org.deplague.todo.application.Effect
import org.deplague.todo.application.Effect.*
import org.deplague.todo.domain.{Description, Status, Title, Todo, TodoId}

import java.time.Instant

class CreateTodoService[F[_]: Effect](savePort: SaveTodoPort[F])
    extends CreateTodoUseCase[F]:
  override def create(command: CreateTodoCommand): F[TodoResponse] =
    for
      title <- Effect.fromEither(Title.create(command.title))
      description <- command.description match
        case Some(d) => Effect.fromEither(Description.create(d))
        case None    => Effect.pure(Description.empty)
      todo = Todo(
        id = TodoId.generate,
        title = title,
        description = description,
        status = Status.Pending,
        createdAt = Instant.now()
      )
      _ <- savePort.save(todo)
    yield TodoResponse.fromDomain(todo)
