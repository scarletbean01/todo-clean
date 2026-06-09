package org.deplague.todo.domain

enum DomainError:
  case ValidationError(message: String)
  case TodoNotFound(id: TodoId)
  case InvalidStateTransition(message: String)
