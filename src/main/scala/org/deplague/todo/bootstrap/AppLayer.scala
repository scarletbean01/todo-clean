package org.deplague.todo.bootstrap

import org.deplague.todo.adapter.in.web.TodoRoutes
import org.deplague.todo.adapter.out.persistence.InMemoryTodoRepository
import org.deplague.todo.application.service.{
  CompleteTodoService,
  CreateTodoService,
  GetTodoService,
  ListTodosService
}
import zio.ZLayer
import zio.http.{Response, Routes}

object AppLayer:
  val live: ZLayer[Any, Nothing, Routes[Any, Response]] =
    InMemoryTodoRepository.live >+>
      CreateTodoService.live >+>
      CompleteTodoService.live >+>
      GetTodoService.live >+>
      ListTodosService.live >+>
      TodoRoutes.live
