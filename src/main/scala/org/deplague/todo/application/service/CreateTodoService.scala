package org.deplague.todo.application.service

import org.deplague.todo.application.dto.{CreateTodoCommand, TodoResponse}
import org.deplague.todo.application.port.in.CreateTodoUseCase
import org.deplague.todo.application.port.out.SaveTodoPort
import org.deplague.todo.domain.{
  Description,
  DomainError,
  Status,
  Title,
  Todo,
  TodoId
}

import zio.ZLayer

import java.time.Instant

class CreateTodoService(savePort: SaveTodoPort) extends CreateTodoUseCase:
  override def create(
      command: CreateTodoCommand
  ): Either[DomainError, TodoResponse] =
    for {
      title <- Title.create(command.title)
      description <- command.description match
        case Some(d) => Description.create(d)
        case None    => Right(Description.empty)
      todo = Todo(
        id = TodoId.generate,
        title = title,
        description = description,
        status = Status.Pending,
        createdAt = Instant.now()
      )
      _ <- savePort.save(todo)
    } yield TodoResponse.fromDomain(todo)

object CreateTodoService:
  val live: ZLayer[SaveTodoPort, Nothing, CreateTodoUseCase] =
    ZLayer.fromFunction(new CreateTodoService(_))
