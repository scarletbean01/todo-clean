package org.deplague.todo.application.port.in

import org.deplague.todo.application.dto.TodoResponse
import org.deplague.todo.domain.DomainError

trait ListTodosUseCase:
  def listAll(): Either[DomainError, List[TodoResponse]]
