package org.deplague.todo.application.service

import org.deplague.todo.application.dto.CompleteTodoCommand
import org.deplague.todo.application.port.out.{LoadTodoPort, SaveTodoPort}
import org.deplague.todo.application.Effect
import org.deplague.todo.domain.{
  Description,
  DomainError,
  Status,
  Title,
  Todo,
  TodoId
}
import zio.test.*

import java.time.Instant
import java.util.UUID

object CompleteTodoServiceSpec extends ZIOSpecDefault:
  type IdEither[A] = Either[DomainError, A]

  given Effect[IdEither] with
    def pure[A](a: A): IdEither[A] = Right(a)
    def map[A, B](fa: IdEither[A])(f: A => B): IdEither[B] = fa.map(f)
    def flatMap[A, B](fa: IdEither[A])(f: A => IdEither[B]): IdEither[B] =
      fa.flatMap(f)
    def fromEither[A](ea: Either[DomainError, A]): IdEither[A] = ea
    def raiseError[A](e: DomainError): IdEither[A] = Left(e)

  def spec = suite("CompleteTodoService")(
    test("should complete a pending todo") {
      val todoId = TodoId.generate
      val port = new FakeLoadAndSavePort[IdEither](Some(todoId, Status.Pending))
      val service = new CompleteTodoService[IdEither](port, port)
      val result = service.complete(CompleteTodoCommand(todoId.value.toString))

      assertTrue(
        result.isRight,
        port.saved.exists(_.status == Status.Completed)
      )
    },
    test("should fail to complete already completed todo") {
      val todoId = TodoId.generate
      val port =
        new FakeLoadAndSavePort[IdEither](Some(todoId, Status.Completed))
      val service = new CompleteTodoService[IdEither](port, port)
      val result = service.complete(CompleteTodoCommand(todoId.value.toString))

      assertTrue(
        result == Left(
          DomainError.InvalidStateTransition("Todo is already completed")
        )
      )
    },
    test("should fail when todo not found") {
      val port = new FakeLoadAndSavePort[IdEither](None)
      val service = new CompleteTodoService[IdEither](port, port)
      val missingId = UUID.randomUUID().toString
      val result = service.complete(CompleteTodoCommand(missingId))

      assertTrue(result.isLeft)
    }
  )

  private class FakeLoadAndSavePort[F[_]: Effect](
      initial: Option[(TodoId, Status)]
  ) extends LoadTodoPort[F]
      with SaveTodoPort[F]:
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

    override def load(id: TodoId): F[Todo] =
      store.get(id.value) match
        case Some(todo) => Effect[F].pure(todo)
        case None       => Effect[F].raiseError(DomainError.TodoNotFound(id))

    override def save(todo: Todo): F[Unit] =
      store = store + (todo.id.value -> todo)
      saved = Some(todo)
      Effect[F].pure(())
