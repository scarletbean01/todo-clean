package org.deplague.todo.bootstrap

import org.deplague.todo.adapter.{TaskE, given}
import org.deplague.todo.adapter.in.web.TodoRoutes
import org.deplague.todo.adapter.out.persistence.InMemoryTodoRepository
import org.deplague.todo.application.port.out.{
  FindTodosPort,
  LoadTodoPort,
  SaveTodoPort
}
import org.deplague.todo.application.service.{
  CompleteTodoService,
  CreateTodoService,
  GetTodoService,
  ListTodosService
}
import zio.{ZIO, ZLayer}
import zio.http.{Response, Routes}

object AppLayer:
  val live: ZLayer[Any, Nothing, Routes[Any, Response]] =
    InMemoryTodoRepository.live >+>
      ZLayer.fromFunction((save: SaveTodoPort[TaskE]) =>
        new CreateTodoService[TaskE](save)
      ) >+>
      ZLayer.fromZIO {
        for {
          load <- ZIO.service[LoadTodoPort[TaskE]]
          save <- ZIO.service[SaveTodoPort[TaskE]]
        } yield new CompleteTodoService[TaskE](load, save)
      } >+>
      ZLayer.fromFunction((load: LoadTodoPort[TaskE]) =>
        new GetTodoService[TaskE](load)
      ) >+>
      ZLayer.fromFunction((find: FindTodosPort[TaskE]) =>
        new ListTodosService[TaskE](find)
      ) >+>
      TodoRoutes.live
