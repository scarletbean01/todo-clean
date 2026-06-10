package org.deplague.todo.application.service

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.application.port.in.ListTodosUseCase
import org.deplague.todo.application.port.out.FindTodosPort
import org.deplague.todo.application.Effect
import org.deplague.todo.application.Effect.*

class ListTodosService[F[_]: Effect](findPort: FindTodosPort[F])
    extends ListTodosUseCase[F]:
  override def listAll(): F[List[TodoResponse]] =
    findPort.findAll().map(_.map(TodoResponse.fromDomain))
