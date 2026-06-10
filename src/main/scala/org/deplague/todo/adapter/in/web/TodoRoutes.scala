package org.deplague.todo.adapter.in.web

import org.deplague.todo.adapter.TaskE
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
import zio.{ZIO, ZLayer}

class TodoRoutes(
    createUseCase: CreateTodoUseCase[TaskE],
    completeUseCase: CompleteTodoUseCase[TaskE],
    getUseCase: GetTodoUseCase[TaskE],
    listUseCase: ListTodosUseCase[TaskE]
):
  val routes: Routes[Any, Response] = Routes(
    Method.POST / "todos" -> handler { (req: Request) =>
      req.body.asString
        .flatMap { body =>
          body.fromJson[CreateTodoRequest] match {
            case Left(error) =>
              ZIO.succeed(
                Response(Status.BadRequest, body = Body.fromString(error))
              )
            case Right(request) =>
              val command =
                CreateTodoCommand(request.title, request.description)
              createUseCase
                .create(command)
                .map { response =>
                  Response.json(TodoDto.fromResponse(response).toJson)
                }
                .catchAll(e => ZIO.succeed(mapDomainError(e)))
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
        completeUseCase
          .complete(command)
          .map { response =>
            Response.json(TodoDto.fromResponse(response).toJson)
          }
          .catchAll(e => ZIO.succeed(mapDomainError(e)))
    },

    Method.GET / "todos" / string("id") -> handler { (id: String, _: Request) =>
      TodoId.fromString(id) match {
        case Left(error)   => ZIO.succeed(mapDomainError(error))
        case Right(todoId) =>
          getUseCase
            .get(todoId)
            .map { response =>
              Response.json(TodoDto.fromResponse(response).toJson)
            }
            .catchAll(e => ZIO.succeed(mapDomainError(e)))
      }
    },

    Method.GET / "todos" -> handler { (_: Request) =>
      listUseCase
        .listAll()
        .map { todos =>
          Response.json(todos.map(TodoDto.fromResponse).toJson)
        }
        .catchAll(e => ZIO.succeed(mapDomainError(e)))
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

object TodoRoutes:
  val live: ZLayer[
    CreateTodoUseCase[TaskE] & CompleteTodoUseCase[TaskE] &
      GetTodoUseCase[TaskE] & ListTodosUseCase[TaskE],
    Nothing,
    Routes[Any, Response]
  ] = ZLayer.fromZIO {
    for {
      createUseCase <- ZIO.service[CreateTodoUseCase[TaskE]]
      completeUseCase <- ZIO.service[CompleteTodoUseCase[TaskE]]
      getUseCase <- ZIO.service[GetTodoUseCase[TaskE]]
      listUseCase <- ZIO.service[ListTodosUseCase[TaskE]]
    } yield new TodoRoutes(
      createUseCase,
      completeUseCase,
      getUseCase,
      listUseCase
    ).routes
  }
