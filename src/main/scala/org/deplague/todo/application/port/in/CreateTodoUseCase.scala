package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.{CreateTodoCommand, TodoResponse}
import org.deplague.todo.domain.DomainError

trait CreateTodoUseCase:
  def create(command: CreateTodoCommand): Either[DomainError, TodoResponse]
