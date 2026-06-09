package org.deplague.todo.application.service

import org.deplague.todo.application.dto.CompleteTodoCommand
import org.deplague.todo.application.port.out.{LoadTodoPort, SaveTodoPort}
import org.deplague.todo.domain.{Description, DomainError, Status, Title, Todo, TodoId}
import zio.test.*

import java.time.Instant
import java.util.UUID

object CompleteTodoServiceSpec extends ZIOSpecDefault:
  def spec = suite("CompleteTodoService")(
    test("should complete a pending todo") {
      val todoId = TodoId.generate
      val port = new FakeLoadAndSavePort(Some(todoId, Status.Pending))
      val service = new CompleteTodoService(port, port)
      val result = service.complete(CompleteTodoCommand(todoId.value.toString))

      assertTrue(
        result.isRight,
        port.saved.exists(_.status == Status.Completed)
      )
    },
    test("should fail to complete already completed todo") {
      val todoId = TodoId.generate
      val port = new FakeLoadAndSavePort(Some(todoId, Status.Completed))
      val service = new CompleteTodoService(port, port)
      val result = service.complete(CompleteTodoCommand(todoId.value.toString))

      assertTrue(
        result == Left(DomainError.InvalidStateTransition("Todo is already completed"))
      )
    },
    test("should fail when todo not found") {
      val port = new FakeLoadAndSavePort(None)
      val service = new CompleteTodoService(port, port)
      val missingId = UUID.randomUUID().toString
      val result = service.complete(CompleteTodoCommand(missingId))

      assertTrue(result.isLeft)
    }
  )

  private class FakeLoadAndSavePort(initial: Option[(TodoId, Status)]) extends LoadTodoPort with SaveTodoPort:
    private var store: Map[UUID, Todo] = initial.map { case (id, status) =>
      id.value -> Todo(
        id = id,
        title = Title.create("Test").toOption.get,
        description = Description.empty,
        status = status,
        createdAt = Instant.now()
      )
    }.toMap

    var saved: Option[Todo] = None

    override def load(id: TodoId): Either[DomainError, Todo] =
      store.get(id.value).toRight(DomainError.TodoNotFound(id))

    override def save(todo: Todo): Either[DomainError, Unit] =
      store = store + (todo.id.value -> todo)
      saved = Some(todo)
      Right(())
