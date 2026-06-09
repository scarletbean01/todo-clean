package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.{CompleteTodoCommand, TodoResponse}
import org.deplague.todo.domain.DomainError

trait CompleteTodoUseCase:
  def complete(command: CompleteTodoCommand): Either[DomainError, TodoResponse]
