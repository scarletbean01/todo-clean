package org.deplague.todo.application.service

import org.deplague.todo.application.dto.{CompleteTodoCommand, TodoResponse}
import org.deplague.todo.application.port.in.CompleteTodoUseCase
import org.deplague.todo.application.port.out.{LoadTodoPort, SaveTodoPort}
import org.deplague.todo.application.Effect
import org.deplague.todo.application.Effect.*
import org.deplague.todo.domain.TodoId

class CompleteTodoService[F[_]: Effect](
    loadPort: LoadTodoPort[F],
    savePort: SaveTodoPort[F]
) extends CompleteTodoUseCase[F]:
  override def complete(command: CompleteTodoCommand): F[TodoResponse] =
    for {
      id <- Effect.fromEither(TodoId.fromString(command.id))
      todo <- loadPort.load(id)
      completed <- Effect.fromEither(todo.complete)
      _ <- savePort.save(completed)
    } yield TodoResponse.fromDomain(completed)
