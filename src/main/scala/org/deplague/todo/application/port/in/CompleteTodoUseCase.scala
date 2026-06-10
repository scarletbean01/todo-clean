package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.{CompleteTodoCommand, TodoResponse}

trait CompleteTodoUseCase[F[_]]:
  def complete(command: CompleteTodoCommand): F[TodoResponse]
