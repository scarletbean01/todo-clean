package org.deplague.todo.application.service

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.application.port.in.ListTodosUseCase
import org.deplague.todo.application.port.out.FindTodosPort
import org.deplague.todo.domain.DomainError

import zio.ZLayer

class ListTodosService(findPort: FindTodosPort) extends ListTodosUseCase:
  override def listAll(): Either[DomainError, List[TodoResponse]] =
    findPort.findAll().map(_.map(TodoResponse.fromDomain))

object ListTodosService:
  val live: ZLayer[FindTodosPort, Nothing, ListTodosUseCase] =
    ZLayer.fromFunction(new ListTodosService(_))
