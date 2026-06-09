package org.deplague.todo.adapter.in.web

import org.deplague.todo.adapter.in.web.request.CreateTodoRequest
import org.deplague.todo.adapter.in.web.response.TodoDto
import org.deplague.todo.application.dto.{
  CompleteTodoCommand,
  CreateTodoCommand
}
import org.deplague.todo.application.port.in.{
  CompleteTodoUseCase,
  CreateTodoUseCase,
  GetTodoUseCase,
  ListTodosUseCase
}
import org.deplague.todo.domain.{DomainError, TodoId}
import zio.http.*
import zio.json.*
import zio.ZIO
import zio.ZLayer

object TodoRoutes:
  def apply(
      createUseCase: CreateTodoUseCase,
      completeUseCase: CompleteTodoUseCase,
      getUseCase: GetTodoUseCase,
      listUseCase: ListTodosUseCase
  ): Routes[Any, Response] = Routes(
    Method.POST / "todos" -> handler { (req: Request) =>
      req.body.asString
        .map { body =>
          body.fromJson[CreateTodoRequest] match {
            case Left(error) =>
              Response(Status.BadRequest, body = Body.fromString(error))
            case Right(request) =>
              val command =
                CreateTodoCommand(request.title, request.description)
              createUseCase.create(command) match {
                case Left(error)     => mapDomainError(error)
                case Right(response) =>
                  Response.json(TodoDto.fromResponse(response).toJson)
              }
          }
        }
        .catchAll(_ =>
          ZIO.succeed(
            Response(
              Status.BadRequest,
              body = Body.fromString("Invalid request body")
            )
          )
        )
    },

    Method.POST / "todos" / string("id") / "complete" -> handler {
      (id: String, _: Request) =>
        val command = CompleteTodoCommand(id)
        ZIO.succeed(completeUseCase.complete(command) match {
          case Left(error)     => mapDomainError(error)
          case Right(response) =>
            Response.json(TodoDto.fromResponse(response).toJson)
        })
    },

    Method.GET / "todos" / string("id") -> handler { (id: String, _: Request) =>
      TodoId.fromString(id) match {
        case Left(error)   => ZIO.succeed(mapDomainError(error))
        case Right(todoId) =>
          ZIO.succeed(getUseCase.get(todoId) match {
            case Left(error)     => mapDomainError(error)
            case Right(response) =>
              Response.json(TodoDto.fromResponse(response).toJson)
          })
      }
    },

    Method.GET / "todos" -> handler { (_: Request) =>
      ZIO.succeed(listUseCase.listAll() match {
        case Left(error)  => mapDomainError(error)
        case Right(todos) =>
          Response.json(todos.map(TodoDto.fromResponse).toJson)
      })
    }
  )

  private def mapDomainError(error: DomainError): Response = error match {
    case DomainError.ValidationError(msg) =>
      Response(Status.BadRequest, body = Body.fromString(msg))
    case DomainError.TodoNotFound(_) =>
      Response(Status.NotFound, body = Body.fromString("Todo not found"))
    case DomainError.InvalidStateTransition(msg) =>
      Response(Status.BadRequest, body = Body.fromString(msg))
  }

  val live: ZLayer[
    CreateTodoUseCase & CompleteTodoUseCase & GetTodoUseCase & ListTodosUseCase,
    Nothing,
    Routes[Any, Response]
  ] = ZLayer.fromZIO:
    for
      createUseCase <- ZIO.service[CreateTodoUseCase]
      completeUseCase <- ZIO.service[CompleteTodoUseCase]
      getUseCase <- ZIO.service[GetTodoUseCase]
      listUseCase <- ZIO.service[ListTodosUseCase]
    yield TodoRoutes(
      createUseCase,
      completeUseCase,
      getUseCase,
      listUseCase
    )
