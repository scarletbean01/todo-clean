package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.TodoResponse

trait ListTodosUseCase[F[_]]:
  def listAll(): F[List[TodoResponse]]
