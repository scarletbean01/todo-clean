package org.deplague.todo.application.service

import org.deplague.todo.application.dto.CreateTodoCommand
import org.deplague.todo.application.port.out.SaveTodoPort
import org.deplague.todo.domain.{Description, DomainError, Status, Title, Todo}
import zio.test.*

import java.time.Instant

object CreateTodoServiceSpec extends ZIOSpecDefault:
  def spec = suite("CreateTodoService")(
    test("should create and save a valid todo") {
      val fakePort = new FakeSavePort()
      val service = new CreateTodoService(fakePort)
      val result = service.create(CreateTodoCommand("Test Title", Some("Test Description")))

      assertTrue(
        result.isRight,
        fakePort.saved.isDefined,
        fakePort.saved.get.title.value == "Test Title",
        fakePort.saved.get.description.value == "Test Description",
        fakePort.saved.get.status == Status.Pending
      )
    },
    test("should create todo with empty description when none provided") {
      val fakePort = new FakeSavePort()
      val service = new CreateTodoService(fakePort)
      val result = service.create(CreateTodoCommand("Test Title", None))

      assertTrue(
        result.isRight,
        fakePort.saved.isDefined,
        fakePort.saved.get.description == Description.empty
      )
    },
    test("should reject empty title") {
      val service = new CreateTodoService(new FakeSavePort())
      val result = service.create(CreateTodoCommand("", None))

      assertTrue(result == Left(DomainError.ValidationError("Title must be non-empty")))
    }
  )

  private class FakeSavePort extends SaveTodoPort:
    var saved: Option[Todo] = None
    override def save(todo: Todo): Either[DomainError, Unit] =
      saved = Some(todo)
      Right(())
