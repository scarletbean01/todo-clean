package org.deplague.todo.application.service

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.application.port.in.GetTodoUseCase
import org.deplague.todo.application.port.out.LoadTodoPort
import org.deplague.todo.application.Effect
import org.deplague.todo.application.Effect.*
import org.deplague.todo.domain.TodoId

class GetTodoService[F[_]: Effect](loadPort: LoadTodoPort[F])
    extends GetTodoUseCase[F]:
  override def get(id: TodoId): F[TodoResponse] =
    loadPort.load(id).map(TodoResponse.fromDomain)
