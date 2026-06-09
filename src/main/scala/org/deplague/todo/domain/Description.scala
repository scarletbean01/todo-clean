package org.deplague.todo.domain

final case class Description private (value: String)

object Description:
  def create(value: String): Either[DomainError.ValidationError, Description] =
    if value.length > 500 then Left(DomainError.ValidationError("Description must be at most 500 characters"))
    else Right(new Description(value))

  val empty: Description = new Description("")
