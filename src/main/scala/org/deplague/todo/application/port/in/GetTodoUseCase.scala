package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.domain.TodoId

trait GetTodoUseCase[F[_]]:
  def get(id: TodoId): F[TodoResponse]
