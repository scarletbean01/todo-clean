package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.{CreateTodoCommand, TodoResponse}

trait CreateTodoUseCase[F[_]]:
  def create(command: CreateTodoCommand): F[TodoResponse]
