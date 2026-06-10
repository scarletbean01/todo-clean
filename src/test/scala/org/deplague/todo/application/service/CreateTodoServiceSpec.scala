package org.deplague.todo.application.service

import org.deplague.todo.application.dto.CreateTodoCommand
import org.deplague.todo.application.port.out.SaveTodoPort
import org.deplague.todo.application.Effect
import org.deplague.todo.domain.{Description, DomainError, Status, Title, Todo}
import zio.test.*

import java.time.Instant

object CreateTodoServiceSpec extends ZIOSpecDefault:
  type IdEither[A] = Either[DomainError, A]

  given Effect[IdEither] with
    def pure[A](a: A): IdEither[A] = Right(a)
    def map[A, B](fa: IdEither[A])(f: A => B): IdEither[B] = fa.map(f)
    def flatMap[A, B](fa: IdEither[A])(f: A => IdEither[B]): IdEither[B] =
      fa.flatMap(f)
    def fromEither[A](ea: Either[DomainError, A]): IdEither[A] = ea
    def raiseError[A](e: DomainError): IdEither[A] = Left(e)

  def spec = suite("CreateTodoService")(
    test("should create and save a valid todo") {
      val fakePort = new FakeSavePort[IdEither]()
      val service = new CreateTodoService[IdEither](fakePort)
      val result = service.create(
        CreateTodoCommand("Test Title", Some("Test Description"))
      )

      assertTrue(
        result.isRight,
        fakePort.saved.isDefined,
        fakePort.saved.get.title.value == "Test Title",
        fakePort.saved.get.description.value == "Test Description",
        fakePort.saved.get.status == Status.Pending
      )
    },
    test("should create todo with empty description when none provided") {
      val fakePort = new FakeSavePort[IdEither]()
      val service = new CreateTodoService[IdEither](fakePort)
      val result = service.create(CreateTodoCommand("Test Title", None))

      assertTrue(
        result.isRight,
        fakePort.saved.isDefined,
        fakePort.saved.get.description == Description.empty
      )
    },
    test("should reject empty title") {
      val service =
        new CreateTodoService[IdEither](new FakeSavePort[IdEither]())
      val result = service.create(CreateTodoCommand("", None))

      assertTrue(
        result == Left(DomainError.ValidationError("Title must be non-empty"))
      )
    }
  )

  private class FakeSavePort[F[_]: Effect] extends SaveTodoPort[F]:
    var saved: Option[Todo] = None
    override def save(todo: Todo): F[Unit] =
      saved = Some(todo)
      Effect[F].pure(())
