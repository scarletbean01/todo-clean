package org.deplague.todo.application.dto

import org.deplague.todo.domain.Todo

case class TodoResponse(
    id: String,
    title: String,
    description: String,
    status: String,
    createdAt: String
)

object TodoResponse:
  def fromDomain(todo: Todo): TodoResponse = TodoResponse(
    id = todo.id.value.toString,
    title = todo.title.value,
    description = todo.description.value,
    status = todo.status.toString,
    createdAt = todo.createdAt.toString
  )
