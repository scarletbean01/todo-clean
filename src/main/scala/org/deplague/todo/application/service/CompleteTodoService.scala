package org.deplague.todo.application.service

import org.deplague.todo.application.dto.{CompleteTodoCommand, TodoResponse}
import org.deplague.todo.application.port.in.CompleteTodoUseCase
import org.deplague.todo.application.port.out.{LoadTodoPort, SaveTodoPort}
import org.deplague.todo.domain.{DomainError, TodoId}

import zio.ZLayer

class CompleteTodoService(loadPort: LoadTodoPort, savePort: SaveTodoPort)
    extends CompleteTodoUseCase:
  override def complete(
      command: CompleteTodoCommand
  ): Either[DomainError, TodoResponse] =
    for {
      id <- TodoId.fromString(command.id)
      todo <- loadPort.load(id)
      completed <- todo.complete
      _ <- savePort.save(completed)
    } yield TodoResponse.fromDomain(completed)

object CompleteTodoService:
  val live: ZLayer[LoadTodoPort & SaveTodoPort, Nothing, CompleteTodoUseCase] =
    ZLayer.fromFunction(new CompleteTodoService(_, _))
