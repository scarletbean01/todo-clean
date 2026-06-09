package org.deplague.todo.domain

import java.util.UUID

final case class TodoId(value: UUID)

object TodoId:
  def generate: TodoId = TodoId(UUID.randomUUID())

  def fromString(value: String): Either[DomainError.ValidationError, TodoId] =
    try Right(TodoId(UUID.fromString(value)))
    catch case _: IllegalArgumentException => Left(DomainError.ValidationError(s"Invalid UUID: $value"))
