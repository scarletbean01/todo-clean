package org.deplague.todo.application.service

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.application.port.in.GetTodoUseCase
import org.deplague.todo.application.port.out.LoadTodoPort
import org.deplague.todo.domain.{DomainError, TodoId}

import zio.ZLayer

class GetTodoService(loadPort: LoadTodoPort) extends GetTodoUseCase:
  override def get(id: TodoId): Either[DomainError, TodoResponse] =
    loadPort.load(id).map(TodoResponse.fromDomain)

object GetTodoService:
  val live: ZLayer[LoadTodoPort, Nothing, GetTodoUseCase] =
    ZLayer.fromFunction(new GetTodoService(_))
