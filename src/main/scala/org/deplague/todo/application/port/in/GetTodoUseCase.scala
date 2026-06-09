package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.domain.{DomainError, TodoId}

trait GetTodoUseCase:
  def get(id: TodoId): Either[DomainError, TodoResponse]
