package org.deplague.todo.adapter.in.web.response

import org.deplague.todo.application.dto.TodoResponse
import zio.json.JsonEncoder

case class TodoDto(
    id: String,
    title: String,
    description: String,
    status: String,
    createdAt: String
) derives JsonEncoder

object TodoDto:
  def fromResponse(response: TodoResponse): TodoDto = TodoDto(
    id = response.id,
    title = response.title,
    description = response.description,
    status = response.status,
    createdAt = response.createdAt
  )
