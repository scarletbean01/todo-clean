package org.deplague.todo.domain

final case class Title private (value: String)

object Title:
  def create(value: String): Either[DomainError.ValidationError, Title] =
    if value.trim.isEmpty then
      Left(DomainError.ValidationError("Title must be non-empty"))
    else if value.length > 100 then
      Left(DomainError.ValidationError("Title must be at most 100 characters"))
    else Right(new Title(value))
