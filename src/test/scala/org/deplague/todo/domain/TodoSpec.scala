package org.deplague.todo.domain

import zio.test.*

import java.time.Instant

object TodoSpec extends ZIOSpecDefault:
  def spec = suite("Todo")(
    test("should complete a pending todo") {
      val todo = createTodo(Status.Pending)
      assertTrue(todo.complete.isRight)
    },
    test("should not complete an already completed todo") {
      val todo = createTodo(Status.Completed)
      assertTrue(
        todo.complete == Left(DomainError.InvalidStateTransition("Todo is already completed"))
      )
    },
    test("completed todo should have Completed status") {
      val todo = createTodo(Status.Pending)
      val completed = todo.complete.toOption.get
      assertTrue(completed.status == Status.Completed)
    }
  )

  private def createTodo(status: Status): Todo =
    Todo(
      id = TodoId.generate,
      title = Title.create("Test").toOption.get,
      description = Description.empty,
      status = status,
      createdAt = Instant.now()
    )
