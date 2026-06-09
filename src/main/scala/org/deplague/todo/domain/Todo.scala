package org.deplague.todo.domain

import java.time.Instant

final case class Todo(
    id: TodoId,
    title: Title,
    description: Description,
    status: Status,
    createdAt: Instant
):
  def complete: Either[DomainError.InvalidStateTransition, Todo] =
    status match
      case Status.Completed => Left(DomainError.InvalidStateTransition("Todo is already completed"))
      case Status.Pending   => Right(copy(status = Status.Completed))
